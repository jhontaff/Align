package com.jet.align.common.exception;

/**
 * El proveedor de email rechazó el envío o no respondió. Es un fallo de
 * infraestructura, no del request del usuario: quien lo atrape decide si
 * lo propaga o lo absorbe (auth lo absorbe para no filtrar si el email existe).
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
