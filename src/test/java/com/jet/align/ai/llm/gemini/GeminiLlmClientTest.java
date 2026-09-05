package com.jet.align.ai.llm.gemini;

import com.jet.align.ai.llm.LlmApiKey;
import com.jet.align.ai.llm.LlmRequest;
import com.jet.align.ai.llm.UserMessage;
import com.jet.align.common.exception.LlmCredentialInvalidException;
import com.jet.align.common.exception.LlmException;
import com.jet.align.common.exception.LlmQuotaExceededException;
import com.jet.align.common.exception.LlmUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Cubre la traducción de errores HTTP del proveedor a las excepciones del
 * dominio, que es de lo que depende que el frontend sepa cuándo abrir el
 * wizard, cuándo solo esperar, y cuándo no molestar al usuario.
 */
class GeminiLlmClientTest {

    private static final String BASE_URL = "http://gemini.test";
    private static final LlmApiKey API_KEY = new LlmApiKey("mi-key");

    private MockRestServiceServer server;
    private GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiLlmClient(builder.build(), new GeminiProperties(BASE_URL, "test-model"));
    }

    @Test
    void validate_acepta_una_key_que_el_proveedor_reconoce() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andExpect(header("x-goog-api-key", "mi-key"))
                .andRespond(withSuccess());

        assertThatCode(() -> client.validate(API_KEY)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void validate_rechaza_la_key_si_el_proyecto_no_tiene_acceso() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.validate(API_KEY))
                .isInstanceOf(LlmCredentialInvalidException.class);
    }

    /**
     * En validate SÍ se interpreta un 400 como "key inválida": el request no
     * lleva payload propio, así que no hay otra cosa a la que culpar.
     */
    @Test
    void validate_rechaza_la_key_ante_cualquier_4xx() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.validate(API_KEY))
                .isInstanceOf(LlmCredentialInvalidException.class);
    }

    @Test
    void validate_distingue_cuota_agotada_de_key_invalida() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.validate(API_KEY))
                .isInstanceOf(LlmQuotaExceededException.class);
    }

    /**
     * Si el proveedor está caído no es culpa de la key: rechazarla haría que el
     * usuario reconfigure una credencial que estaba perfecta.
     */
    @Test
    void validate_no_culpa_a_la_key_si_el_proveedor_esta_caido() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.validate(API_KEY))
                .isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    void chat_traduce_403_a_credencial_invalida() {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.chat(aRequest(), API_KEY))
                .isInstanceOf(LlmCredentialInvalidException.class);
    }

    /**
     * Al revés que validate: en chat un 400 suele ser un request mal armado por
     * nosotros (un schema de tool inválido, por ejemplo). Mapearlo a credencial
     * inválida mandaría al usuario a reconfigurar una key que está bien.
     */
    @Test
    void chat_no_culpa_a_la_key_ante_un_400() {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.chat(aRequest(), API_KEY))
                .isInstanceOf(LlmException.class)
                .isNotInstanceOf(LlmCredentialInvalidException.class);
    }

    @Test
    void chat_traduce_429_a_cuota_agotada() {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.chat(aRequest(), API_KEY))
                .isInstanceOf(LlmQuotaExceededException.class);
    }

    private static LlmRequest aRequest() {
        return new LlmRequest(List.of(new UserMessage("hola")), List.of());
    }
}
