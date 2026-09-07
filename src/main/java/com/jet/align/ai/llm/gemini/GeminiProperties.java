package com.jet.align.ai.llm.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración del proveedor Gemini, leída de application.properties
 * (prefijo {@code align.gemini}).
 *
 * <p>{@code apiKeys} es el pool de keys del operador: el chat usa una de estas,
 * elegida al azar, y reintenta con otra si Gemini responde 429/403. La idea de
 * que cada usuario traiga su propia key (BYOK) está construida pero dormida
 * (ver {@code ai.credential} y CLAUDE.md); reactivarla es volver a cablear
 * {@code AgentServiceImpl} a {@code LlmCredentialService.resolve()}.
 *
 * <p>Los valores de {@code apiKeys} son secretos: en prod llegan por la env var
 * {@code ALIGN_GEMINI_API_KEYS} (CSV), nunca en un .properties versionado.
 */
@ConfigurationProperties(prefix = "align.gemini")
public record GeminiProperties(
        String baseUrl,
        String model,
        List<String> apiKeys
) {
}
