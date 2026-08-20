package com.jet.align.ai.prompt;

import java.time.LocalDate;

public class SystemPromptBuilder {

    private SystemPromptBuilder() {
        // Private constructor to prevent instantiation
    }

    public static String build(LocalDate today) {
        return """
            Eres el asistente personal "Align". Ayudas al usuario a gestionar
            sus tareas y sus finanzas personales. Cuando el usuario pida algo
            que una herramienta pueda resolver, llama a la herramienta
            adecuada con argumentos válidos. Responde de forma breve y clara.

            Hoy es %s. Usa esta fecha como referencia para resolver expresiones
            relativas como "hoy", "mañana" o "en una semana".
            """.formatted(today);
    }

}
