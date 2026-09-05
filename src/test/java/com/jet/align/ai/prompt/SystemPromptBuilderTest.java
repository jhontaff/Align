package com.jet.align.ai.prompt;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptBuilderTest {

    @Test
    void el_prompt_incluye_la_fecha_y_hora_recibida_en_el_contexto() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 2, 14, 35);

        String prompt = SystemPromptBuilder.build(new UserContext(now, List.of()));

        assertThat(prompt).contains(now.toString());
    }

    @Test
    void el_prompt_incluye_cada_memoria_como_bullet_cuando_hay_memorias() {
        List<String> memories = List.of("Prefiere que le hable de usted", "Vive en Lima");

        String prompt = SystemPromptBuilder.build(new UserContext(LocalDateTime.of(2026, 8, 2, 14, 35), memories));

        assertThat(prompt)
                .contains("- Prefiere que le hable de usted")
                .contains("- Vive en Lima");
    }

    @Test
    void el_prompt_instruye_ordenar_eventos_tareas_y_habitos_cronologicamente_sin_inventar_horas() {
        String prompt = SystemPromptBuilder.build(new UserContext(LocalDateTime.of(2026, 8, 2, 14, 35), List.of()));

        assertThat(prompt)
                .contains("ordenados cronológicamente")
                .contains("No inventes horas");
    }


    // El bloque de memorias no debería aparecer si el usuario todavía no tiene
    // ninguna guardada -- mostrar un encabezado seguido de nada sería ruido, no
    // contexto útil para el LLM.
    @Test
    void el_prompt_no_agrega_el_bloque_de_memorias_si_la_lista_esta_vacia() {
        String prompt = SystemPromptBuilder.build(new UserContext(LocalDateTime.of(2026, 8, 2, 14, 35), List.of()));

        assertThat(prompt).doesNotContain("Esto es lo que recordás sobre el usuario");
    }

    @Test
    void el_prompt_permite_consejos_practicos_pero_limita_el_asesoramiento_profesional() {
        String prompt = SystemPromptBuilder.build(new UserContext(LocalDateTime.of(2026, 8, 2, 14, 35), List.of()));

        assertThat(prompt)
                .contains("Está bien dar consejos prácticos basados en los datos del usuario")
                .contains("asesoramiento profesional que requiera una licencia");
    }
}
