package com.jet.align.ai.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmApiKeyTest {

    /**
     * La razón de ser del tipo: un record normal imprime todos sus componentes
     * en toString(), así que cualquier log que tocara el objeto filtraría la
     * key en claro.
     */
    @Test
    void toString_no_expone_el_valor_de_la_key() {
        LlmApiKey apiKey = new LlmApiKey("AIzaSyEsteEsElSecreto");

        assertThat(apiKey.toString()).doesNotContain("AIzaSyEsteEsElSecreto");
        assertThat(apiKey.value()).isEqualTo("AIzaSyEsteEsElSecreto");
    }

    @Test
    void rechaza_una_key_vacia_o_en_blanco() {
        assertThatThrownBy(() -> new LlmApiKey("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new LlmApiKey(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
