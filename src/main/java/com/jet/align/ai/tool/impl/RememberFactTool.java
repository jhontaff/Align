package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.memory.UserMemoryService;
import com.jet.align.ai.memory.dto.MemoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RememberFactTool implements Tool<MemoryResponse>{

    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "content": {
              "type": "string",
              "description": "The fact, preference, or context to remember about the user, written as a short, self-contained statement.",
              "maxLength": 500
            }
          },
          "required": ["content"],
          "additionalProperties": false
        }
        """;

    @Override
    public String name() {
        return "remember_fact";
    }

    @Override
    public String description() {
        return "Saves a durable fact, preference, or piece of context about the user for future conversations. "
                + "Use it when the user explicitly asks to be remembered, or clearly shares something worth recalling later "
                + "(e.g. a stable preference or biographical detail). Do not use it for things that already belong to an "
                + "existing domain — savings goals belong in Finance, sleep routines belong in Habit, and so on.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(PARAMETERS_SCHEMA, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON Schema for tool " + name(), e);
        }
    }

    @Override
    public ToolResult<MemoryResponse> execute(ToolContext context) {
        String content = (String) context.arguments().get("content");
        MemoryResponse response = userMemoryService.remember(context.user(), content);
        return new ToolResult<>(response, "Memory saved successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }

}
