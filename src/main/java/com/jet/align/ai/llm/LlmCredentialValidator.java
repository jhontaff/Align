package com.jet.align.ai.llm;

/**
 * Verifica contra el proveedor que una API key sirve, antes de guardarla.
 *
 * <p>Va separado de {@link LlmClient} a propósito: son responsabilidades y
 * consumidores distintos (el agente conversa, el servicio de credenciales
 * valida), y mantenerlo aparte deja a {@code LlmClient} como interfaz de un
 * solo método. Un mismo adapter de proveedor implementa las dos.
 */
public interface LlmCredentialValidator {

    /**
     * @throws com.jet.align.common.exception.LlmCredentialInvalidException si el
     *         proveedor rechaza la key.
     */
    void validate(LlmApiKey apiKey);

}
