package com.jet.align.ai.llm.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

final class GeminiApi {

    private GeminiApi() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateContentRequest(
            List<Content> contents,
            SystemInstruction systemInstruction,
            List<Tool> tools
    ) {
    }

    record SystemInstruction(List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Content(String role, List<Part> parts) {
    }

    /**
     * Un solo tipo para los tres part que usamos (text, functionCall,
     * functionResponse): con {@code NON_NULL} cada uno serializa solo sus
     * campos relevantes, e ignoramos los que no reconocemos al leer la
     * respuesta.
     *
     * <p>{@code thoughtSignature} viaja como hermano de {@code functionCall}
     * (no dentro de él): Gemini lo exige de vuelta tal cual en el siguiente
     * turno o rechaza la petición con 400 INVALID_ARGUMENT.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text, FunctionCall functionCall, FunctionResponse functionResponse, String thoughtSignature) {
        Part(String text, FunctionCall functionCall, FunctionResponse functionResponse) {
            this(text, functionCall, functionResponse, null);
        }
    }

    record FunctionCall(String name, Map<String, Object> args) {
    }

    record FunctionResponse(String name, Map<String, Object> response) {
    }

    record Tool(List<FunctionDeclaration> functionDeclarations) {
    }

    record FunctionDeclaration(String name, String description, Map<String, Object> parameters) {
    }

    // --- Respuesta ----------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateContentResponse(List<Candidate> candidates) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Candidate(Content content) {
        }
    }
}
