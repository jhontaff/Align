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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateMemoryTool implements Tool<MemoryResponse>{

    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "memoryId": {
              "type": "string",
              "description": "The unique identifier of the memory to update, obtained from a prior call to list_memories."
            },
            "content": {
              "type": "string",
              "description": "The new content that fully replaces the existing memory.",
              "maxLength": 500
            }
          },
          "required": ["memoryId", "content"],
          "additionalProperties": false
        }
        """;

    @Override
    public String name() {
        return "update_memory";
    }

    @Override
    public String description() {
        return "Updates the content of an existing memory, given its exact id. You must call list_memories first if you "
                + "don't already know the memoryId. Only call this when you are confident which existing memory matches what "
                + "the user wants to change — if there is any ambiguity about which one they mean, ask the user to clarify "
                + "instead of guessing.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(PARAMETERS_SCHEMA, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON Schema for tool " + name(), e);
        }
    }

    @Override
    public ToolResult<MemoryResponse> execute(ToolContext context) {
        UUID memoryId = UUID.fromString((String) context.arguments().get("memoryId"));
        String content = (String) context.arguments().get("content");
        MemoryResponse response = userMemoryService.update(context.user(), memoryId, content);
        return new ToolResult<>(response, "Memory updated successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.DESTRUCTIVE;
    }

}
