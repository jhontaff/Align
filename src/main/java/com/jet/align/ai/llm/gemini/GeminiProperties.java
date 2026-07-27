package com.jet.align.ai.llm.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del proveedor Gemini, leída de application.properties
 * (prefijo {@code align.gemini}). La API key nunca va en el código: llega
 * por variable de entorno.
 */
@ConfigurationProperties(prefix = "align.gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String model
) {
}
