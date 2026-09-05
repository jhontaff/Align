package com.jet.align.ai.credential.dto;

import java.time.Instant;

/**
 * Estado de la credencial de un usuario. Nunca expone la API key: solo si hay
 * una configurada, sus últimos 4 caracteres (para que el usuario reconozca
 * cuál es) y cuándo se guardó por última vez.
 */
public record LlmCredentialStatusResponse(
        boolean configured,
        String lastFour,
        Instant updatedAt
) {

    public static LlmCredentialStatusResponse notConfigured() {
        return new LlmCredentialStatusResponse(false, null, null);
    }
}
