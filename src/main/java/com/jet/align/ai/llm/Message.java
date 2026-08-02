package com.jet.align.ai.llm;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Un turno dentro de una conversación con el modelo, de forma
 * independiente del proveedor.
 *
 * <p>Es {@code sealed}: solo estos cuatro tipos pueden existir, así que
 * al mapear la conversación hacia un proveedor concreto un {@code switch}
 * exhaustivo nos obliga a cubrirlos todos (el compilador falla si olvidamos
 * uno).
 *
 * <p>Las anotaciones Jackson son necesarias porque {@link com.jet.align.ai.memory}
 * persiste el historial serializando {@code List<Message>} a JSON: sin la
 * marca de tipo, al deserializar se perdería qué subtipo era cada mensaje.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SystemMessage.class, name = "system"),
        @JsonSubTypes.Type(value = UserMessage.class, name = "user"),
        @JsonSubTypes.Type(value = AssistantMessage.class, name = "assistant"),
        @JsonSubTypes.Type(value = ToolMessage.class, name = "tool")
})
public sealed interface Message
        permits SystemMessage, UserMessage, AssistantMessage, ToolMessage {
}
