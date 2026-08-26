package com.jet.align.ai.llm;

import java.util.List;

/**
 * La respuesta del modelo en un turno.
 *
 * <p>Puede contener texto ({@code content}), una o varias peticiones de
 * herramientas ({@code toolCalls}), o ambos. Cuando el modelo decide
 * llamar herramientas, {@code content} suele venir vacío; cuando responde
 * directamente, {@code toolCalls} viene vacía.
 */
public record   AssistantMessage(
        String content,
        List<ToolCall> toolCalls
) implements Message {

    public AssistantMessage {
        // Nunca dejamos toolCalls en null: simplifica al consumidor
        // (puede hacer toolCalls().isEmpty() sin comprobar null) y la
        // lista queda inmutable.
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
