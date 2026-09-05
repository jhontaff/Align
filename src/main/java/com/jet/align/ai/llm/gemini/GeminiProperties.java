package com.jet.align.ai.llm.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del proveedor Gemini, leída de application.properties
 * (prefijo {@code align.gemini}).
 *
 * <p>Acá ya no hay ninguna API key: desde BYOK, la key la aporta cada usuario
 * y viaja por request ({@code LlmApiKey}), no por configuración.
 */
@ConfigurationProperties(prefix = "align.gemini")
public record GeminiProperties(
        String baseUrl,
        String model
) {
}
