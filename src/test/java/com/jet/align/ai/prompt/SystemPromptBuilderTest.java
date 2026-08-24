package com.jet.align.ai.prompt;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptBuilderTest {

    @Test
    void el_prompt_incluye_la_fecha_recibida_en_el_contexto() {
        LocalDate today = LocalDate.of(2026, 8, 2);

        String prompt = SystemPromptBuilder.build(new UserContext(today, List.of()));

        assertThat(prompt).contains(today.toString());
    }

    @Test
    void el_prompt_incluye_cada_memoria_como_bullet_cuando_hay_memorias() {
        List<String> memories = List.of("Prefiere que le hable de usted", "Vive en Lima");

        String prompt = SystemPromptBuilder.build(new UserContext(LocalDate.of(2026, 8, 2), memories));

        assertThat(prompt)
                .contains("- Prefiere que le hable de usted")
                .contains("- Vive en Lima");
    }

    // El bloque de memorias no debería aparecer si el usuario todavía no tiene
    // ninguna guardada -- mostrar un encabezado seguido de nada sería ruido, no
    // contexto útil para el LLM.
    @Test
    void el_prompt_no_agrega_el_bloque_de_memorias_si_la_lista_esta_vacia() {
        String prompt = SystemPromptBuilder.build(new UserContext(LocalDate.of(2026, 8, 2), List.of()));

        assertThat(prompt).doesNotContain("Esto es lo que recordás sobre el usuario");
    }
}
