package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.task.TaskService;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.enums.TaskStatus;
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
    public String description() {
        return "Lists the authenticated user's tasks, optionally filtered by status.";
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
    public ToolResult<List<TaskResponse>> execute(ToolContext context) {
        String statusArg = (String) context.arguments().get("status");
        TaskStatus status = statusArg != null ? TaskStatus.valueOf(statusArg) : null;
        Pageable pageable = PageRequest.of(0 , 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<TaskResponse> tasks = taskService.getTasks(context.user(),pageable, status).getContent();
        return new ToolResult<>(tasks, "Tasks retrieved successfully");
    }
}
