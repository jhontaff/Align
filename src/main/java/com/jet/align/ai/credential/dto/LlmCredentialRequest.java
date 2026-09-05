package com.jet.align.ai.credential.dto;

import jakarta.validation.constraints.NotBlank;

public record LlmCredentialRequest(

        @NotBlank(message = "La API key es obligatoria.")
        String apiKey
) {}
