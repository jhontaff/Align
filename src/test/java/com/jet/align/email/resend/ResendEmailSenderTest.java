package com.jet.align.email.resend;

import com.jet.align.common.exception.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {

    private static final String BASE_URL = "http://resend.test";
    private static final ResendProperties PROPERTIES =
            new ResendProperties(BASE_URL, "re_test_key", "Align <no-reply@align.test>");

    private MockRestServiceServer server;
    private ResendEmailSender sender;

    @BeforeEach
    void setUp() {
        // Mismo cableado que ResendConfig: el header Authorization va en el
        // cliente, así que el test también lo verifica.
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + PROPERTIES.apiKey());
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new ResendEmailSender(builder.build(), PROPERTIES);
    }

    @Test
    void send_hace_post_a_emails_con_el_payload_y_la_key() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test_key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from").value("Align <no-reply@align.test>"))
                .andExpect(jsonPath("$.to[0]").value("ana@example.com"))
                .andExpect(jsonPath("$.subject").value("Asunto"))
                .andExpect(jsonPath("$.html").value("<p>Hola</p>"))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> sender.send("ana@example.com", "Asunto", "<p>Hola</p>"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void send_traduce_un_4xx_a_EmailDeliveryException() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"from no verificado\"}"));

        assertThatThrownBy(() -> sender.send("ana@example.com", "Asunto", "<p>Hola</p>"))
                .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void send_traduce_un_5xx_a_EmailDeliveryException() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> sender.send("ana@example.com", "Asunto", "<p>Hola</p>"))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
