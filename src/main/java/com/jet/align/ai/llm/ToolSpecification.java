package com.jet.align.ai.llm;

import java.util.Map;

/**
 * La descripción de una herramienta <em>tal como se la presentamos al
 * modelo</em>: nombre, para qué sirve y el esquema de sus argumentos.
 *
 * <p>Es deliberadamente una foto plana, sin {@code execute()} ni ninguna
 * dependencia del dominio. Así el paquete {@code ai.llm} puede describir
 * herramientas al proveedor sin conocer {@link com.jet.align.ai.tool.Tool}
 * (que sí toca el dominio). El agente será quien traduzca cada {@code Tool}
 * a este DTO al construir la petición.
 *
 * <p>{@code parameters} es un JSON Schema representado como objeto JSON.
 */
public record ToolSpecification(
        String name,
        String description,
        Map<String, Object> parameters
) {

    public ToolSpecification {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
