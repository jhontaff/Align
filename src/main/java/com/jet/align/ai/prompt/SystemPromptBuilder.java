package com.jet.align.ai.prompt;

import java.time.LocalDate;
import java.util.stream.Collectors;

public class SystemPromptBuilder {

    private SystemPromptBuilder() {
        // Private constructor to prevent instantiation
    }

    public static String build(UserContext context) {
        String memoriesBlock = context.memories().isEmpty()
                ? ""
                : "\nEsto es lo que recordás sobre el usuario:\n"
                + context.memories().stream().map(m -> "- " + m).collect(Collectors.joining("\n"))
                + "\n";

        return """
            Eres el asistente personal "Align". Ayudas al usuario con su vida
            personal usando las herramientas disponibles. Responde breve y claro.
            Hoy es %s. Usalo para resolver fechas relativas ("hoy", "mañana",
            "en una semana").
            %s""".formatted(context.today(), memoriesBlock);
    }


}
