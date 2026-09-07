package com.jet.align.ai.llm;

/**
 * Conversa con el proveedor de LLM.
 *
 * <p>No recibe credencial: hoy la key la aporta el pool del operador dentro del
 * adapter del proveedor. Si vuelve BYOK (una key por usuario), esta firma pasa a
 * {@code chat(LlmRequest, LlmApiKey)} y el agente resuelve la key por request.
 */
public interface LlmClient {

    LlmResponse chat(LlmRequest request);

}
