package com.jet.align.email.resend;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Resend (prefijo {@code align.email}).
 *
 * <p>{@code apiKey} es secreto: en prod llega por {@code ALIGN_RESEND_API_KEY},
 * nunca en un .properties versionado. {@code from} tiene que ser un remitente
 * verificado en la cuenta de Resend, si no la API responde 4xx.
 */
@ConfigurationProperties(prefix = "align.email")
public record ResendProperties(
        String baseUrl,
        String apiKey,
        String from
) {
}
