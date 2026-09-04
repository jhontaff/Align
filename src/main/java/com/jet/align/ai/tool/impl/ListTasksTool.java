package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.task.TaskService;
import com.jet.align.task.dto.TaskFilter;
import com.jet.align.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListTasksTool implements Tool<List<TaskResponse>> {

    private final TaskService taskService;
    private  final ObjectMapper objectMapper;
    private static final String PARAMETERS_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "status": {
              "type": "string",
              "enum": ["PENDING", "IN_PROGRESS", "COMPLETED"],
              "description": "Optional filter by task status."
            },
            "dueFrom": {
              "type": "string",
              "format": "date",
              "description": "Optional inclusive lower bound on the task's due date (YYYY-MM-DD). Compute it from the current date for ranges like 'esta semana'."
            },
            "dueTo": {
              "type": "string",
              "format": "date",
              "description": "Optional inclusive upper bound on the task's due date (YYYY-MM-DD)."
            }
          },
          "required": [],
          "additionalProperties": false
        }
        """;


    @Override
    public String name() {
        return "list_tasks";
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
    public String description() {
        return "Lists the authenticated user's tasks, optionally filtered by status and/or a due-date range "
                + "(dueFrom/dueTo). Compute the range yourself from the current date-time in the system prompt.";
    }
    @Override
    public ToolResult<List<TaskResponse>> execute(ToolContext context) {
        TaskFilter filter = objectMapper.convertValue(context.arguments(), TaskFilter.class);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<TaskResponse> tasks = taskService.getTasks(context.user(), pageable, filter).getContent();
        return new ToolResult<>(tasks, "Tasks retrieved successfully");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
