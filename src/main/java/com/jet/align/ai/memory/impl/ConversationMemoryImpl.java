package com.jet.align.ai.memory.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.llm.Message;
import com.jet.align.ai.memory.ConversationHistory;
import com.jet.align.ai.memory.ConversationHistoryRepository;
import com.jet.align.ai.memory.ConversationMemory;
import com.jet.align.common.exception.AgentException;
import com.jet.align.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationMemoryImpl implements ConversationMemory {

    private final ConversationHistoryRepository conversationHistoryRepository;
    private final ObjectMapper objectMapper ;


    public ConversationMemoryImpl(ConversationHistoryRepository conversationHistoryRepository, ObjectMapper objectMapper) {
        this.conversationHistoryRepository = conversationHistoryRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly= true)
    public List<Message> loadHistory(User user) {
        return conversationHistoryRepository.findByUser(user)
                .map(history -> readMessages(history.getHistoryJson()))
                .orElse(List.of());
    }


    // Lectura-modificación-escritura sin lock: aceptable porque hoy es
    // un solo usuario interactuando secuencialmente. Si se agrega
    // concurrencia real (multi-tab, reintentos), revisar con @Version.
    @Override
    @Transactional
    public void append(User user, List<Message> newMessages) {
        Optional<ConversationHistory> existing = conversationHistoryRepository.findByUser(user);
        ConversationHistory history;

        if (existing.isEmpty()) {
            history = new ConversationHistory();
            history.setUser(user);
            history.setHistoryJson(writeMessages(newMessages));
            conversationHistoryRepository.save(history);
        } else {
            history = existing.get();
            String currentMessagesString = history.getHistoryJson();
            List<Message> currentMessages = readMessages(currentMessagesString);

            currentMessages.addAll(newMessages);

            history.setHistoryJson(writeMessages(currentMessages));
            conversationHistoryRepository.save(history);
        }
    }

    private List<Message> readMessages(String json){
        try {
            return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
        } catch (Exception e) {
            throw new AgentException("Failed to deserialize conversation history", e);
        }
    }


    private String writeMessages(List<Message> messages){
        try {
            return objectMapper.writerFor(new TypeReference<List<Message>>() {})
                    .writeValueAsString(messages);
        } catch (Exception e) {
            throw new AgentException("Failed to serialize conversation history", e);
        }
    }

}
