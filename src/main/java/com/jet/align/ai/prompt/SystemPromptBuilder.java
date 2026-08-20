package com.jet.align.ai.prompt;

import java.time.LocalDate;

public class SystemPromptBuilder {

    private SystemPromptBuilder() {
        // Private constructor to prevent instantiation
    }

    public static String build(LocalDate today) {
        return """
                Eres el asistente personal "Align". Ayudas al usuario con su vida
                personal usando las herramientas disponibles. Responde breve y claro.
                Hoy es %s. Usalo para resolver fechas relativas ("hoy", "mañana",
                "en una semana").
                """.formatted(today);
    }

}
