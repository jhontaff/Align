package com.jet.align.common.exception;

/**
 * La key es válida pero agotó su cuota (429 del proveedor). Se traduce a 429:
 * el frontend NO debe lanzar el wizard, porque no hay nada que reconfigurar.
 */
public class LlmQuotaExceededException extends LlmException {

    public LlmQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
