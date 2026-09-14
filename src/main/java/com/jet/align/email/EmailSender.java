package com.jet.align.email;

/**
 * Contrato neutral de envío de email. Ningún consumidor (auth, futuros
 * dominios) depende del proveedor concreto: la implementación vive en
 * {@code email.<provider>}, mismo criterio que {@code ai.llm.<provider>}.
 */
public interface EmailSender {

    void send(String to, String subject, String htmlBody);
}
