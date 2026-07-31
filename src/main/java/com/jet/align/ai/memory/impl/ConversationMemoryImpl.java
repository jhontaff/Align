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

import java.util.ArrayList;
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
        Optional<ConversationHistory> existing = conversationHistoryRepository.findByUser(user);
        if (existing.isEmpty()) {
            return List.of();
        } else {
            ConversationHistory history = existing.get();
            String messagesJson = history.getHistoryJson();
            try {
                return objectMapper.readValue(messagesJson, new TypeReference<List<Message>>() {});
            } catch (Exception e) {
                throw new AgentException("Failed to deserialize conversation history");
            }
        }
    }

    @Override
    @Transactional
    public void append(User user, List<Message> newMessages) {
        Optional<ConversationHistory> existing = conversationHistoryRepository.findByUser(user);
        ConversationHistory history;

        if (existing.isEmpty()) {
            history = new ConversationHistory();
            history.setUser(user);
            try {
                String newMessagessString = objectMapper.writeValueAsString(newMessages);
                history.setHistoryJson(newMessagessString);
            } catch (Exception e) {
                throw new AgentException("Failed to serialize new messages");
            }
            conversationHistoryRepository.save(history);
        } else {
            history = existing.get();
            String currentMessagesString = history.getHistoryJson();
            List<Message> currentMessagesJson = new ArrayList<>();
            try {
                currentMessagesJson = objectMapper.readValue(currentMessagesString, new TypeReference<List<Message>>() {
                });
            } catch (Exception e) {
                throw new AgentException("Failed to deserialize existing conversation history");
            }
            currentMessagesJson.addAll(newMessages);
            try {
                currentMessagesString = objectMapper.writeValueAsString(currentMessagesJson);
            } catch (Exception e) {
                throw new AgentException("Failed to serialize updated messages");
            }
            history.setHistoryJson(currentMessagesString);
            conversationHistoryRepository.save(history);
        }
    }

}
