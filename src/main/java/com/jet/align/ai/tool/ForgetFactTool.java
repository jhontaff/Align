package com.jet.align.ai.tool;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.memory.UserMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ForgetFactTool implements Tool<Void> {

    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "memoryId": {
              "type": "string",
              "description": "The unique identifier of the memory to delete, obtained from a prior call to list_memories."
            }
          },
          "required": ["memoryId"],
          "additionalProperties": false
        }
        """;

    @Override
    public String description() {
        return "Deletes a memory permanently, given its id. If you don't already know the exact memoryId, call "
                + "list_memories first to find it.";
    }

    @Override
    public ToolResult<Void> execute(ToolContext context) {
        UUID memoryId = UUID.fromString((String) context.arguments().get("memoryId"));
        userMemoryService.forget(context.user(), memoryId);
        return new ToolResult<>(null, "Memory deleted successfully.");
    }


    @Override
    public String name() {
        return "forget_fact";
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
    public RiskLevel risk() {
        return RiskLevel.DESTRUCTIVE;
    }

}
