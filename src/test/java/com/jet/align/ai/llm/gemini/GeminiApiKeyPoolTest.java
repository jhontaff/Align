package com.jet.align.ai.llm.gemini;

import com.jet.align.ai.llm.LlmApiKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiApiKeyPoolTest {

    private static GeminiApiKeyPool poolOf(List<String> keys) {
        return new GeminiApiKeyPool(new GeminiProperties("http://x", "m", keys));
    }

    @Test
    void falla_al_arrancar_si_no_hay_ninguna_key() {
        assertThatThrownBy(() -> poolOf(List.of()))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> poolOf(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ignora_entradas_vacias_y_recorta_espacios() {
        GeminiApiKeyPool pool = poolOf(List.of("  key-a  ", "", "   ", "key-b"));

        assertThat(pool.size()).isEqualTo(2);
        assertThat(pool.shuffled())
                .containsExactlyInAnyOrder(new LlmApiKey("key-a"), new LlmApiKey("key-b"));
    }

    @Test
    void shuffled_devuelve_todas_las_keys_una_sola_vez() {
        GeminiApiKeyPool pool = poolOf(List.of("k1", "k2", "k3"));

        List<LlmApiKey> shuffled = pool.shuffled();

        assertThat(shuffled).containsExactlyInAnyOrder(
                new LlmApiKey("k1"), new LlmApiKey("k2"), new LlmApiKey("k3"));
    }
}
