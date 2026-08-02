package com.jet.align.ai.prompt;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptBuilderTest {

    // build() antes vivía cacheado en un campo static final de AgentServiceImpl,
    // calculado una sola vez al arrancar el proceso. Con el server corriendo varios
    // días, "hoy" seguía siendo el día del arranque para siempre, y el LLM no podía
    // resolver bien expresiones relativas ("mañana", "en una semana").
    //
    // Ahora build(LocalDate) recibe la fecha como parámetro y AgentServiceImpl la
    // reconstruye en cada chat() con LocalDate.now(). Estos tests no prueban esa
    // parte (no hay caching que probar acá, es un método puro) — prueban el
    // contrato que hace posible el fix: que el texto realmente cambia según la
    // fecha recibida, en vez de ser fijo.

    @Test
    void el_prompt_incluye_la_fecha_recibida_como_parametro() {
        LocalDate today = LocalDate.of(2026, 8, 2);

        String prompt = SystemPromptBuilder.build(today);

        assertThat(prompt).contains(today.toString());
    }

    @Test
    void fechas_distintas_producen_prompts_distintos() {
        // Si esto fallara (mismo texto para dos fechas distintas), build() habría
        // vuelto a ignorar el parámetro "today" -- el mismo problema de fondo que
        // el caching original, solo que reintroducido de otra forma.
        String prompt1 = SystemPromptBuilder.build(LocalDate.of(2026, 8, 2));
        String prompt2 = SystemPromptBuilder.build(LocalDate.of(2026, 12, 25));

        assertThat(prompt1).isNotEqualTo(prompt2);
    }
}
