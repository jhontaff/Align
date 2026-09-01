package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.task.TaskService;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.dto.TaskUpdateRequest;
import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateTaskTool implements Tool<TaskResponse> {

    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "taskId": {
                  "type": "string",
                  "description": "The unique identifier of the task to update."
                },
                "title": {
                  "type": "string",
                  "description": "Short, clear title of the task.",
                  "maxLength": 100
                },
                "description": {
                  "type": "string",
                  "description": "Optional details about the task.",
                  "maxLength": 1000
                },
                "status":{
                    "type": "string",
                    "enum": ["PENDING", "IN_PROGRESS", "COMPLETED"],
                    "description": "Current state of the task."
                    },
                "priority": {
                  "type": "string",
                  "enum": ["LOW", "MEDIUM", "HIGH"],
                  "description": "How urgent the task is."
                },
                "dueDate": {
                  "type": "string",
                  "format": "date",
                  "description": "Optional due date in ISO-8601 format (YYYY-MM-DD)."
                },
                 "dueTime": {
                  "type": "string",
                  "format": "time",
                  "description": "Optional due time in HH:mm format (24-hour)."
                }
              },
              "required": ["taskId"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "update_task";
    }

    @Override
    public String description() {
        return "Updates an existing task for the authenticated user.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(
                    PARAMETERS_SCHEMA,
                    new TypeReference<>() {}
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse parameters schema", e);
        }

    }

    // taskId viaja en el mismo Map<String, Object>, pero se lee aparte, arriba en
    // execute(); ignoreUnknown evita que convertValue() explote por ese campo
    // extra que el patch no necesita.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TaskPatch(String title, String description, TaskStatus status,
                             Priority priority, LocalDate dueDate, LocalTime dueTime) {}

    @Override
    public ToolResult<TaskResponse> execute(ToolContext context) {
        UUID taskId = UUID.fromString((String) context.arguments().get("taskId"));
        TaskResponse current = taskService.getTaskById(taskId, context.user());

        TaskPatch taskPatch = objectMapper.convertValue(context.arguments(), TaskPatch.class);

        TaskUpdateRequest merged = new TaskUpdateRequest(
                taskPatch.title() != null ? taskPatch.title() : current.title(),
                taskPatch.description() != null ? taskPatch.description() : current.description(),
                taskPatch.status() != null ? taskPatch.status() : current.status(),
                taskPatch.priority() != null ? taskPatch.priority() : current.priority(),
                taskPatch.dueDate() != null ? taskPatch.dueDate() : current.dueDate(),
                taskPatch.dueTime() != null ? taskPatch.dueTime() : current.dueTime()
        );
        TaskResponse updated = taskService.updateTask(taskId, merged, context.user());
        return new ToolResult<>(updated, "Task updated successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
