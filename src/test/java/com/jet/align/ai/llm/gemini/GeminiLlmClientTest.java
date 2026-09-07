package com.jet.align.ai.llm.gemini;

import com.jet.align.ai.llm.LlmApiKey;
import com.jet.align.ai.llm.LlmRequest;
import com.jet.align.ai.llm.LlmResponse;
import com.jet.align.ai.llm.UserMessage;
import com.jet.align.common.exception.LlmCredentialInvalidException;
import com.jet.align.common.exception.LlmException;
import com.jet.align.common.exception.LlmQuotaExceededException;
import com.jet.align.common.exception.LlmUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Cubre dos cosas:
 * <ul>
 *   <li>{@code chat}: el pool de keys — reintento ante 429/403, propagación sin
 *       reintento de 5xx/400, y agotamiento a {@link LlmUnavailableException}.</li>
 *   <li>{@code validate} (BYOK dormido): traducción de errores HTTP para el
 *       endpoint {@code /api/ai/credentials}.</li>
 * </ul>
 */
class GeminiLlmClientTest {

    private static final String BASE_URL = "http://gemini.test";
    private static final LlmApiKey API_KEY = new LlmApiKey("mi-key");
    private static final String SUCCESS_BODY =
            "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"hola\"}]}}]}";

    private MockRestServiceServer server;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private GeminiLlmClient clientWith(String... keys) {
        GeminiProperties properties = new GeminiProperties(BASE_URL, "test-model", List.of(keys));
        return new GeminiLlmClient(builder.build(), properties, new GeminiApiKeyPool(properties));
    }

    // --- chat: pool de keys -------------------------------------------------

    @Test
    void chat_devuelve_la_respuesta_cuando_la_key_funciona() {
        server.expect(requestTo(containsString("generateContent")))
                .andExpect(header("x-goog-api-key", "k1"))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        LlmResponse response = clientWith("k1").chat(aRequest());

        assertThat(response.message().content()).isEqualTo("hola");
        server.verify();
    }

    @Test
    void chat_reintenta_con_otra_key_si_una_da_429() {
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        LlmResponse response = clientWith("k1", "k2").chat(aRequest());

        assertThat(response.message().content()).isEqualTo("hola");
        server.verify();
    }

    @Test
    void chat_reintenta_con_otra_key_si_una_da_403() {
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        LlmResponse response = clientWith("k1", "k2").chat(aRequest());

        assertThat(response.message().content()).isEqualTo("hola");
        server.verify();
    }

    @Test
    void chat_tira_no_disponible_si_todas_las_keys_se_agotan() {
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> clientWith("k1", "k2").chat(aRequest()))
                .isInstanceOf(LlmUnavailableException.class);

        server.verify();
    }

    /**
     * Un 400 suele ser un request mal armado por nosotros: probar otra key no
     * cambia nada. Se propaga tras el primer intento, sin reintentar.
     */
    @Test
    void chat_no_reintenta_ante_un_400_y_lo_propaga() {
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> clientWith("k1", "k2").chat(aRequest()))
                .isInstanceOf(LlmException.class)
                .isNotInstanceOf(LlmCredentialInvalidException.class);

        server.verify();
    }

    /**
     * Gemini caído (5xx): otra key tampoco ayuda. Se propaga sin reintentar.
     */
    @Test
    void chat_no_reintenta_ante_un_5xx_y_lo_propaga() {
        server.expect(ExpectedCount.once(), requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> clientWith("k1", "k2").chat(aRequest()))
                .isInstanceOf(LlmUnavailableException.class);

        server.verify();
    }

    // --- validate: BYOK dormido ------------------------------------------

    @Test
    void validate_acepta_una_key_que_el_proveedor_reconoce() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andExpect(header("x-goog-api-key", "mi-key"))
                .andRespond(withSuccess());

        assertThatCode(() -> clientWith("mi-key").validate(API_KEY)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void validate_rechaza_la_key_si_el_proyecto_no_tiene_acceso() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> clientWith("mi-key").validate(API_KEY))
                .isInstanceOf(LlmCredentialInvalidException.class);
    }

    @Test
    void validate_rechaza_la_key_ante_cualquier_4xx() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> clientWith("mi-key").validate(API_KEY))
                .isInstanceOf(LlmCredentialInvalidException.class);
    }

    @Test
    void validate_distingue_cuota_agotada_de_key_invalida() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> clientWith("mi-key").validate(API_KEY))
                .isInstanceOf(LlmQuotaExceededException.class);
    }

    @Test
    void validate_no_culpa_a_la_key_si_el_proveedor_esta_caido() {
        server.expect(requestTo(BASE_URL + "/models"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> clientWith("mi-key").validate(API_KEY))
                .isInstanceOf(LlmUnavailableException.class);
    }

    private static LlmRequest aRequest() {
        return new LlmRequest(List.of(new UserMessage("hola")), List.of());
    }
}
