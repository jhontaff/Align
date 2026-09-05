package com.jet.align.ai.credential;

import com.jet.align.ai.credential.dto.LlmCredentialStatusResponse;
import com.jet.align.ai.llm.LlmApiKey;
import com.jet.align.user.User;

public interface LlmCredentialService {

    LlmCredentialStatusResponse save(User user, String rawApiKey);

    LlmCredentialStatusResponse getStatus(User user);

    void delete(User user);

    /**
     * Devuelve la key descifrada para usarla contra el proveedor.
     *
     * @throws com.jet.align.common.exception.LlmCredentialMissingException si el
     *         usuario no tiene credencial configurada o la guardada no se pudo
     *         descifrar.
     */
    LlmApiKey resolve(User user);
}
