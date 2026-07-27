package com.jet.align.ai.model;

import java.util.Map;

public record ToolCall(

        String id,

        String name,

        Map<String, Object> arguments,

        /**
         * Token opaco que algunos proveedores (p. ej. Gemini 3, como
         * "thought signature") exigen recibir de vuelta intacto junto al
         * function call en el siguiente turno. Null para proveedores que no
         * lo usan.
         */
        String signature

) {

    public ToolCall(String id, String name, Map<String, Object> arguments) {
        this(id, name, arguments, null);
    }
}
