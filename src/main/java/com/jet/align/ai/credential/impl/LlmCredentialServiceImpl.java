package com.jet.align.ai.credential.impl;

import com.jet.align.ai.credential.LlmCredential;
import com.jet.align.ai.credential.LlmCredentialRepository;
import com.jet.align.ai.credential.LlmCredentialService;
import com.jet.align.ai.credential.dto.LlmCredentialStatusResponse;
import com.jet.align.ai.llm.LlmApiKey;
import com.jet.align.ai.llm.LlmCredentialValidator;
import com.jet.align.common.exception.LlmCredentialMissingException;
import com.jet.align.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LlmCredentialServiceImpl implements LlmCredentialService {

    private static final String NOT_CONFIGURED_MESSAGE =
            "No tenés una API key configurada. Configurala para poder usar el chat.";

    private static final int LAST_FOUR_LENGTH = 4;

    private final LlmCredentialRepository llmCredentialRepository;
    private final LlmCredentialValidator llmCredentialValidator;
    private final ApiKeyCipher apiKeyCipher;

    public LlmCredentialServiceImpl(LlmCredentialRepository llmCredentialRepository,
                                    LlmCredentialValidator llmCredentialValidator,
                                    ApiKeyCipher apiKeyCipher) {
        this.llmCredentialRepository = llmCredentialRepository;
        this.llmCredentialValidator = llmCredentialValidator;
        this.apiKeyCipher = apiKeyCipher;
    }

    /**
     * Valida contra el proveedor ANTES de persistir: si la key no sirve, no
     * queda nada guardado y el usuario se entera en el mismo paso del wizard,
     * no la primera vez que intente chatear.
     */
    @Override
    @Transactional
    public LlmCredentialStatusResponse save(User user, String rawApiKey) {
        // Pegar una key desde el navegador arrastra espacios y saltos de línea
        // con muchísima frecuencia; recortarlos acá evita un "key inválida"
        // incomprensible para el usuario.
        String apiKey = rawApiKey == null ? "" : rawApiKey.trim();
        LlmApiKey candidate = new LlmApiKey(apiKey);

        llmCredentialValidator.validate(candidate);

        LlmCredential credential = llmCredentialRepository.findByUser(user)
                .orElseGet(() -> {
                    LlmCredential fresh = new LlmCredential();
                    fresh.setUser(user);
                    return fresh;
                });

        credential.setEncryptedKey(apiKeyCipher.encrypt(apiKey));
        credential.setLastFour(lastFourOf(apiKey));

        return toStatus(llmCredentialRepository.save(credential));
    }

    /**
     * Devuelve {@code configured=false} en vez de 404 cuando no hay credencial:
     * el wizard consulta este endpoint justamente para saber si hace falta
     * configurar algo, así que "no hay" es una respuesta normal, no un error.
     */
    @Override
    public LlmCredentialStatusResponse getStatus(User user) {
        return llmCredentialRepository.findByUser(user)
                .map(this::toStatus)
                .orElseGet(LlmCredentialStatusResponse::notConfigured);
    }

    /**
     * Idempotente: borrar una credencial que no existe no es un error, es el
     * estado que el usuario pidió. Mismo criterio que completeHabit o subscribe.
     */
    @Override
    @Transactional
    public void delete(User user) {
        llmCredentialRepository.findByUser(user).ifPresent(llmCredentialRepository::delete);
    }

    @Override
    public LlmApiKey resolve(User user) {
        LlmCredential credential = llmCredentialRepository.findByUser(user)
                .orElseThrow(() -> new LlmCredentialMissingException(NOT_CONFIGURED_MESSAGE));

        // Un descifrado fallido (típicamente, rotaron align.crypto.secret)
        // desemboca en el mismo camino que "no hay key": el frontend abre el
        // wizard y el usuario la vuelve a cargar. No hay nada que recuperar.
        return apiKeyCipher.decrypt(credential.getEncryptedKey())
                .map(LlmApiKey::new)
                .orElseThrow(() -> new LlmCredentialMissingException(NOT_CONFIGURED_MESSAGE));
    }

    private LlmCredentialStatusResponse toStatus(LlmCredential credential) {
        return new LlmCredentialStatusResponse(
                true,
                credential.getLastFour(),
                credential.getUpdatedAt());
    }

    private String lastFourOf(String apiKey) {
        return apiKey.length() <= LAST_FOUR_LENGTH
                ? apiKey
                : apiKey.substring(apiKey.length() - LAST_FOUR_LENGTH);
    }
}
