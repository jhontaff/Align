package com.jet.align.common.exception;

/**
 * El proveedor rechazó la API key del usuario (inválida, revocada, o proyecto
 * sin acceso). Se traduce a 409 para distinguirla de "nunca la configuró".
 */
public class LlmCredentialInvalidException extends LlmException {

    public LlmCredentialInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
