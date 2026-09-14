package com.jet.align.email.resend;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Arma el {@link RestClient} que habla con Resend.
 *
 * <p>A diferencia de Gemini, acá hay una sola API key (no un pool), así que
 * va como header por defecto del cliente en vez de agregarse por request.
 */
@Configuration
@EnableConfigurationProperties(ResendProperties.class)
class ResendConfig {

    @Bean
    RestClient resendRestClient(RestClient.Builder builder, ResendProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }
}
