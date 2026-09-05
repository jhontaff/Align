package com.jet.align.ai.llm;

/**
 * Credencial neutral con la que se autentica una llamada al proveedor de LLM.
 *
 * <p>Existe como tipo propio, y no como un {@code String} suelto, por una razón
 * concreta: {@code toString()} está sobrescrito para enmascarar el valor. Un
 * record normal imprime todos sus componentes, así que cualquier log que
 * imprimiera el objeto filtraría la key en claro.
 */
public record LlmApiKey(String value) {

    public LlmApiKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La API key no puede estar vacía.");
        }
    }

    @Override
    public String toString() {
        return "LlmApiKey[****]";
    }
}
