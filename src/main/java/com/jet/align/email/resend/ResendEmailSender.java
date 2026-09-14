package com.jet.align.email.resend;

import com.jet.align.common.exception.EmailDeliveryException;
import com.jet.align.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final RestClient resendRestClient;
    private final ResendProperties properties;

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            resendRestClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResendApi.SendEmailRequest(properties.from(), List.of(to), subject, htmlBody))
                    .retrieve()
                    .body(ResendApi.SendEmailResponse.class);

        } catch (RestClientResponseException e) {
            // Un 4xx acá es configuración nuestra (from no verificado, key
            // inválida), no algo que el usuario pueda corregir: detalle al log,
            // excepción genérica hacia arriba.
            log.error("Resend rechazó el email. status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmailDeliveryException("No se pudo enviar el email.", e);

        } catch (ResourceAccessException e) {
            throw new EmailDeliveryException("No se pudo conectar con el servicio de email.", e);
        }
    }
}
