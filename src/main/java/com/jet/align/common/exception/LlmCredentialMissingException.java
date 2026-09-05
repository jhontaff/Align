package com.jet.align.common.exception;

/**
 * El usuario todavía no configuró su API key, o la guardada quedó ilegible.
 * Se traduce a 428 para que el frontend lance el wizard de configuración.
 */
public class LlmCredentialMissingException extends LlmException {

    public LlmCredentialMissingException(String message) {
        super(message);
    }
}
