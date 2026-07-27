package com.jet.align.ai.llm;

/**
 * El resultado de ejecutar una herramienta, devuelto al modelo para que
 * continúe el razonamiento.
 *
 * <p>{@code toolCallId} apunta al {@code id} del {@link com.jet.align.ai.model.ToolCall}
 * que originó esta ejecución: así el modelo empareja cada resultado con la
 * petición correcta cuando pidió varias herramientas a la vez.
 */
public record ToolMessage(
        String toolCallId,
        String content
) implements Message {
}
