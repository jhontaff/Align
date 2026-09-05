package com.jet.align.ai.llm.gemini;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Arma el {@link RestClient} que habla con Gemini.
 *
 * <p>Gemini autentica con la cabecera {@code x-goog-api-key} (no
 * {@code Bearer} ni {@code x-api-key}).
 *
 * <p>Solo se activa cuando {@code align.llm.provider=gemini}: así evita
 * competir con los {@code RestClient} de OpenAI/Anthropic por el mismo tipo
 * de bean.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
@ConditionalOnProperty(prefix = "align.llm", name = "provider", havingValue = "gemini")
class GeminiConfig {

    @Bean
    RestClient geminiRestClient(RestClient.Builder builder, GeminiProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }
}

