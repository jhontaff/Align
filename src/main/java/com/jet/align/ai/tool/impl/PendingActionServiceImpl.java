package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.tool.*;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PendingActionServiceImpl implements PendingActionService {

    private final PendingActionRepository repository;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final int ttlHours;

    public PendingActionServiceImpl (PendingActionRepository repository,
                                     ToolRegistry toolRegistry,
                                     ObjectMapper objectMapper,
                                     @Value("${align.pending-action.ttl-hours}") int ttlHours) {
        this.repository = repository;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.ttlHours = ttlHours;
    }

    @Override
    @Transactional
    public ToolResult<?> confirm(User user, UUID id) {
        PendingAction pending = repository.findByIdAndUser(id, user).
                orElseThrow(() -> new ResourceNotFoundException("Pending action not found: " + id));
        if (pending.getStatus() != PendingActionStatus.PENDING) {
            throw new BusinessException("This pending action was already " + pending.getStatus() + ".");
        }
        Tool<?> tool = toolRegistry.get(pending.getToolName())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown tool: " + pending.getToolName()));

        ToolResult<?> result = tool.execute(new ToolContext(user, deserialize(pending.getArgumentsJson())));
        pending.setStatus(PendingActionStatus.CONFIRMED);
        return result;
    }

    @Override
    @Transactional
    public PendingAction create(User user, String toolName, Map<String, Object> arguments) {
        PendingAction pendingAction = new PendingAction();
        pendingAction.setUser(user);
        pendingAction.setToolName(toolName);
        pendingAction.setStatus(PendingActionStatus.PENDING);
        try {
            pendingAction.setArgumentsJson(objectMapper.writeValueAsString(arguments));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize arguments for pending action", e);
        }
        return repository.save(pendingAction);
    }


    private Map<String, Object> deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize arguments for pending action", e);
        }
    }

    @Override
    @Transactional
    public void reject(User user, UUID id) {
        PendingAction pending = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Pending action not found: " + id));
        if (pending.getStatus() != PendingActionStatus.PENDING) {
            throw new BusinessException("This pending action was already " + pending.getStatus() + ".");
        }
        pending.setStatus(PendingActionStatus.REJECTED);
        repository.save(pending);
    }

    @Override
    @Transactional
    public void expireStale(){
        Instant cutoff = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
        List<PendingAction> stale = repository.findByStatusAndCreatedAtBefore(
                PendingActionStatus.PENDING, cutoff);
        for (PendingAction action : stale) {
            action.setStatus(PendingActionStatus.EXPIRED);
        }
        repository.saveAll(stale);
    }
}
