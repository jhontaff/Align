package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.memory.UserMemoryService;
import com.jet.align.ai.memory.dto.MemoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListMemoriesTool implements  Tool<List<MemoryResponse>>
{

    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
        {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": false
        }
        """;

    @Override
    public String name() {
        return "list_memories";
    }

    @Override
    public String description() {
        return "Lists everything currently remembered about the authenticated user. Call this before update_memory "
                + "or forget_fact to find the id of the memory you need to target, or when the user asks what you remember about them.";
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
    public ToolResult<List<MemoryResponse>> execute(ToolContext context) {
        List<MemoryResponse> memories = userMemoryService.list(context.user());
        return new ToolResult<>(memories, "Memories retrieved successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
