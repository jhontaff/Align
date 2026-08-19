package com.jet.align.ai.agent.dto;

import com.jet.align.ai.agent.AgentService;

/**
 * La respuesta final del agente al usuario, tras completar el bucle.
 *
 * <p>Envoltorio de un solo campo a propósito: es la frontera pública del
 * agente, y aquí crecerán más adelante metadatos útiles (herramientas
 * ejecutadas, consumo de tokens, traza de pasos) sin cambiar la firma de
 * {@link AgentService}.
 */
public record AgentResponse(String reply) {
}
