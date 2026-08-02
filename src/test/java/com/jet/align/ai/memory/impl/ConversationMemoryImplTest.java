package com.jet.align.ai.memory.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.llm.AssistantMessage;
import com.jet.align.ai.llm.Message;
import com.jet.align.ai.llm.UserMessage;
import com.jet.align.ai.memory.ConversationHistory;
import com.jet.align.ai.memory.ConversationHistoryRepository;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConversationMemoryImplTest {

    /**
     * Regresión del bug real que sufrimos: {@code append} serializaba con
     * {@code writeValueAsString(Object)}, que pierde el tipo genérico
     * declarado y con él el discriminador {@code "type"} que exige
     * {@link Message}. El resultado quedaba guardado sin ese campo y
     * {@code loadHistory} no podía reconstruir qué subtipo era cada mensaje.
     *
     * <p>El repository se mockea: lo que se prueba es que lo que
     * {@code append} escribe, {@code loadHistory} lo puede leer de vuelta
     * preservando el tipo concreto de cada mensaje, no el mapeo JPA.
     */
    @Test
    void el_historial_persistido_se_recupera_preservando_el_tipo_concreto_de_cada_mensaje() {
        ConversationHistoryRepository repository = mock(ConversationHistoryRepository.class);
        ConversationMemoryImpl memory = new ConversationMemoryImpl(repository, new ObjectMapper());

        User user = new User();
        ConversationHistory[] stored = new ConversationHistory[1];

        when(repository.findByUser(user)).thenAnswer(invocation ->
                Optional.ofNullable(stored[0]));
        when(repository.save(any(ConversationHistory.class))).thenAnswer(invocation -> {
            stored[0] = invocation.getArgument(0);
            return stored[0];
        });

        List<Message> turn = List.of(
                new UserMessage("hola"),
                new AssistantMessage("¿en qué ayudo?", List.of()));

        memory.append(user, turn);

        assertThat(memory.loadHistory(user)).isEqualTo(turn);
    }
}
