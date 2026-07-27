package com.jet.align.ai.llm;

/**
 * Instrucciones base que definen el comportamiento del asistente.
 * Normalmente es el primer mensaje de la conversación.
 */
public record SystemMessage(String content) implements Message {
}
