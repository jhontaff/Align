package com.jet.align.ai.llm.gemini;

import com.jet.align.ai.llm.AssistantMessage;
import com.jet.align.ai.llm.LlmApiKey;
import com.jet.align.ai.llm.LlmClient;
import com.jet.align.ai.llm.LlmCredentialValidator;
import com.jet.align.ai.llm.LlmRequest;
import com.jet.align.ai.llm.LlmResponse;
import com.jet.align.ai.llm.Message;
import com.jet.align.ai.llm.SystemMessage;
import com.jet.align.ai.llm.ToolMessage;
import com.jet.align.ai.llm.UserMessage;
import com.jet.align.ai.llm.ToolCall;
import com.jet.align.common.exception.LlmCredentialInvalidException;
import com.jet.align.common.exception.LlmException;
import com.jet.align.common.exception.LlmQuotaExceededException;
import com.jet.align.common.exception.LlmUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "align.llm", name = "provider", havingValue = "gemini")
public class GeminiLlmClient implements LlmClient, LlmCredentialValidator {

    private static final String API_KEY_HEADER = "x-goog-api-key";

    private final RestClient geminiRestClient;
    private final GeminiProperties properties;

    /**
     * Gemini solo soporta un subconjunto del schema OpenAPI 3.0 (no JSON
     * Schema completo): claves como {@code additionalProperties} o
     * {@code maxLength}, válidas para OpenAI/Anthropic, hacen que la API
     * responda 400 INVALID_ARGUMENT. Filtramos los parámetros del tool a
     * las claves que Gemini realmente entiende.
     */
    private static final Set<String> ALLOWED_SCHEMA_KEYS = Set.of(
            "type", "format", "description", "nullable", "enum", "properties", "required", "items", "propertyOrdering");

    @Override
    public LlmResponse chat(LlmRequest request, LlmApiKey apiKey) {
        try {
            GeminiApi.GenerateContentResponse response = geminiRestClient.post()
                    .uri("/models/{model}:generateContent", properties.model())
                    .header(API_KEY_HEADER, apiKey.value())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toGeminiRequest(request))
                    .retrieve()
                    .body(GeminiApi.GenerateContentResponse.class);

            return toLlmResponse(response);

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new LlmQuotaExceededException(
                    "Tu API key de Gemini agotó la cuota disponible. Probá de nuevo más tarde.", e);

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // 401/403 señalan a la credencial sin ambigüedad. Un 400 NO se mapea
            // acá: Gemini también devuelve 400 cuando el request está mal armado
            // (un schema de tool inválido, por ejemplo), y culpar a la key del
            // usuario por un bug nuestro lo mandaría a reconfigurar algo que
            // está bien.
            throw new LlmCredentialInvalidException(
                    "Gemini rechazó tu API key. Volvé a configurarla.", e);

        } catch (HttpServerErrorException e) {
            throw new LlmUnavailableException(
                    "Gemini no está disponible en este momento.", e);

        } catch (RestClientResponseException e) {
            throw new LlmException("Gemini rechazó la solicitud: " + e.getMessage(), e);
        }
    }

    /**
     * Valida la key con un GET al listado de modelos: es la llamada más barata
     * que ejerce la autenticación (no consume cuota de generación) y ya detecta
     * los dos casos que importan, key inválida y proyecto sin acceso.
     *
     * <p>Acá cualquier 4xx sí se interpreta como "la key no sirve", al revés que
     * en {@link #chat}: este request no lleva payload que podamos haber armado
     * mal, así que no hay otra explicación posible.
     */
    @Override
    public void validate(LlmApiKey apiKey) {
        try {
            geminiRestClient.get()
                    .uri("/models")
                    .header(API_KEY_HEADER, apiKey.value())
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new LlmQuotaExceededException(
                    "La API key es válida pero está sin cuota disponible en este momento.", e);

        } catch (HttpClientErrorException e) {
            throw new LlmCredentialInvalidException(
                    "La API key no es válida o su proyecto no tiene acceso a Gemini.", e);

        } catch (HttpServerErrorException e) {
            // El proveedor está caído: no es culpa de la key, no la rechacemos.
            throw new LlmUnavailableException(
                    "No se pudo verificar la API key porque Gemini no está disponible.", e);
        }
    }

    // --- salida: nuestro contrato -> formato Gemini ----------------------

    private GeminiApi.GenerateContentRequest toGeminiRequest(LlmRequest request) {
        String system = request.messages().stream()
                .filter(SystemMessage.class::isInstance)
                .map(message -> ((SystemMessage) message).content())
                .collect(Collectors.joining("\n"));

        List<GeminiApi.Content> contents = request.messages().stream()
                .filter(message -> !(message instanceof SystemMessage))
                .map(this::toGeminiContent)
                .toList();

        List<GeminiApi.Tool> tools = request.tools().isEmpty()
                ? List.of()
                : List.of(new GeminiApi.Tool(request.tools().stream()
                        .map(spec -> new GeminiApi.FunctionDeclaration(spec.name(), spec.description(), sanitizeSchema(spec.parameters())))
                        .toList()));

        GeminiApi.SystemInstruction systemInstruction = system.isBlank()
                ? null
                : new GeminiApi.SystemInstruction(List.of(new GeminiApi.Part(system, null, null)));

        return new GeminiApi.GenerateContentRequest(contents, systemInstruction, tools);
    }

    private GeminiApi.Content toGeminiContent(Message message) {
        // Exhaustivo sobre la interfaz sellada; SystemMessage ya se filtró antes.
        return switch (message) {
            case SystemMessage ignored -> throw new IllegalStateException("SystemMessage debe filtrarse antes");
            case UserMessage(String content) -> new GeminiApi.Content(
                    "user", List.of(new GeminiApi.Part(content, null, null)));
            case AssistantMessage assistant -> new GeminiApi.Content("model", assistantParts(assistant));
            case ToolMessage(String toolCallId, String content) -> new GeminiApi.Content(
                    "user", List.of(new GeminiApi.Part(
                            null, null, new GeminiApi.FunctionResponse(toolCallId, Map.of("content", content)))));
        };
    }

    private List<GeminiApi.Part> assistantParts(AssistantMessage assistant) {
        List<GeminiApi.Part> parts = new ArrayList<>();
        if (assistant.content() != null && !assistant.content().isBlank()) {
            parts.add(new GeminiApi.Part(assistant.content(), null, null));
        }
        for (ToolCall call : assistant.toolCalls()) {
            parts.add(new GeminiApi.Part(
                    null, new GeminiApi.FunctionCall(call.name(), call.arguments()), null, call.signature()));
        }
        return parts;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitizeSchema(Map<String, Object> schema) {
        if (schema == null) {
            return null;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            if (!ALLOWED_SCHEMA_KEYS.contains(entry.getKey())) {
                continue;
            }
            Object value = entry.getValue();
            if ("properties".equals(entry.getKey()) && value instanceof Map<?, ?> properties) {
                Map<String, Object> sanitizedProperties = new LinkedHashMap<>();
                for (Map.Entry<?, ?> property : properties.entrySet()) {
                    if (property.getValue() instanceof Map<?, ?> propertySchema) {
                        sanitizedProperties.put((String) property.getKey(), sanitizeSchema((Map<String, Object>) propertySchema));
                    }
                }
                sanitized.put(entry.getKey(), sanitizedProperties);
            } else if ("items".equals(entry.getKey()) && value instanceof Map<?, ?> itemsSchema) {
                sanitized.put(entry.getKey(), sanitizeSchema((Map<String, Object>) itemsSchema));
            } else {
                sanitized.put(entry.getKey(), value);
            }
        }
        return sanitized;
    }

    // --- entrada: formato Gemini -> nuestro contrato ----------------------

    private LlmResponse toLlmResponse(GeminiApi.GenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new LlmException("Gemini no devolvió ninguna respuesta.");
        }

        GeminiApi.Content content = response.candidates().getFirst().content();
        StringBuilder text = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();

        if (content != null && content.parts() != null) {
            for (GeminiApi.Part part : content.parts()) {
                if (part.text() != null) {
                    text.append(part.text());
                } else if (part.functionCall() != null) {
                    // Gemini no da un id de tool call, solo el nombre de la
                    // función: lo reusamos como id para que el ToolMessage
                    // de vuelta pueda emparejarse (functionResponse.name).
                    GeminiApi.FunctionCall call = part.functionCall();
                    toolCalls.add(new ToolCall(call.name(), call.name(), call.args(), part.thoughtSignature()));
                }
            }
        }

        return new LlmResponse(new AssistantMessage(text.toString(), toolCalls));
    }
}
