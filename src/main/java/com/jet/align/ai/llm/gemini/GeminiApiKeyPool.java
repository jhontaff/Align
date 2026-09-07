package com.jet.align.ai.llm.gemini;

import com.jet.align.ai.llm.LlmApiKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pool de API keys de Gemini del operador.
 *
 * <p>El chat toma una key de acá al azar; si Gemini la rechaza por cuota (429)
 * o por acceso (403), {@link GeminiLlmClient} reintenta con otra. La selección
 * es sin estado compartido (un {@code shuffle} por llamada, no un cursor), así
 * que no hay carrera entre requests concurrentes.
 *
 * <p>Fail-fast: si {@code align.gemini.api-keys} queda vacío la app no arranca,
 * mismo criterio que {@code ApiKeyCipher} sin {@code align.crypto.secret}.
 */
@Component
@ConditionalOnProperty(prefix = "align.llm", name = "provider", havingValue = "gemini")
class GeminiApiKeyPool {

    private final List<LlmApiKey> keys;

    GeminiApiKeyPool(GeminiProperties properties) {
        List<String> raw = properties.apiKeys() == null ? List.of() : properties.apiKeys();
        this.keys = raw.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(LlmApiKey::new)
                .toList();

        if (this.keys.isEmpty()) {
            throw new IllegalStateException(
                    "align.gemini.api-keys está vacío: el asistente necesita al menos una API key de Gemini.");
        }
    }

    int size() {
        return keys.size();
    }

    /**
     * Todas las keys en orden aleatorio. El caller las recorre en orden
     * probando cada una una sola vez.
     */
    List<LlmApiKey> shuffled() {
        List<LlmApiKey> copy = new ArrayList<>(keys);
        Collections.shuffle(copy);
        return copy;
    }
}
