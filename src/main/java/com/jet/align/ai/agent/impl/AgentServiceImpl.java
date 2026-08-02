package com.jet.align.ai.agent.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.agent.AgentResponse;
import com.jet.align.ai.agent.AgentService;
import com.jet.align.ai.agent.execution.ToolExecutionService;
import com.jet.align.ai.llm.*;
import com.jet.align.ai.memory.ConversationMemory;
import com.jet.align.ai.model.ToolCall;
import com.jet.align.ai.prompt.SystemPromptBuilder;
import com.jet.align.ai.tool.ToolRegistry;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.common.exception.AgentException;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

     //Tope de vueltas del bucle
    private static final int MAX_STEPS = 8;

    private static final String SYSTEM_PROMPT = SystemPromptBuilder.build();

    private final LlmClient llmClient;
    private final ToolExecutionService toolExecutionService;
    private final ToolRegistry toolRegistry;
    private final ConversationMemory conversationMemory;
    private final ObjectMapper objectMapper;

    @Override
    public AgentResponse chat(String userMessage, User user) {

        List<Message> messages = new ArrayList<>();
        UserMessage userTurn = new UserMessage(userMessage);

        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.addAll(conversationMemory.loadHistory(user));
        messages.add(userTurn);

        List<ToolSpecification> tools = buildSpecifications();

        for (int step = 0; step < MAX_STEPS; step++) {
            LlmResponse response = llmClient.chat(new LlmRequest(messages, tools));
            AssistantMessage assistant = response.message();

            messages.add(assistant);

            if (!assistant.hasToolCalls()) {
                conversationMemory.append(user, List.of(userTurn, assistant));
                return new AgentResponse(assistant.content());
            }


            for (ToolCall call : assistant.toolCalls()) {
                messages.add(runTool(call, user));
            }
        }

        throw new AgentException(
                "El agente superó el máximo de pasos (" + MAX_STEPS + ").");
    }


    private ToolMessage runTool(ToolCall call, User user) {
        try {
            ToolResult<?> result = toolExecutionService.execute(call, user);
            return new ToolMessage(call.id(), toJson(result));
        } catch (BusinessException | ResourceNotFoundException e) {
            return new ToolMessage(call.id(), toJson(Map.of("error", e.getMessage())));
        } catch (Exception e) {
            log.error("La tool '{}' falló inesperadamente (call id={})", call.name(), call.id(), e);
            return new ToolMessage(call.id(), toJson(Map.of(
                    "error", "No se pudo completar la operación solicitada.")));
        }
    }


    private List<ToolSpecification> buildSpecifications() {
        return toolRegistry.getAll().stream()
                .map(tool -> new ToolSpecification(
                        tool.name(),
                        tool.description(),
                        tool.parameters()))
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new AgentException("No se pudo serializar el resultado de la tool: "
                    + e.getMessage());
        }
    }
}
