package com.jet.align.email.resend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Formato de wire de Resend. No sale de este paquete. */
final class ResendApi {

    private ResendApi() {
    }

    record SendEmailRequest(String from, List<String> to, String subject, String html) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendEmailResponse(String id) {
    }
}
