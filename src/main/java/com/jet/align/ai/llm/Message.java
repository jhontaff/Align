package com.jet.align.ai.llm;

/**
 * Un turno dentro de una conversación con el modelo, de forma
 * independiente del proveedor.
 *
 * <p>Es {@code sealed}: solo estos cuatro tipos pueden existir, así que
 * al mapear la conversación hacia un proveedor concreto un {@code switch}
 * exhaustivo nos obliga a cubrirlos todos (el compilador falla si olvidamos
 * uno).
 */
public sealed interface Message
        permits SystemMessage, UserMessage, AssistantMessage, ToolMessage {
}
