package com.jet.align.ai.tool;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.task.TaskService;
import com.jet.align.task.dto.TaskRequest;
import com.jet.align.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreateTaskTool implements Tool<TaskResponse> {

    private static final Logger log = LoggerFactory.getLogger(CreateTaskTool.class);
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
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
              "required": ["title", "priority"],
              "additionalProperties": false
            }
            """;


    @Override
    public String name() {
        return "create_task";
    }

    @Override
    public String description() {
        return "Creates a new task for the authenticated user.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(
                    PARAMETERS_SCHEMA,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Invalid JSON Schema for tool " + name(), e);
        }
    }

    @Override
    public ToolResult<TaskResponse> execute(ToolContext context) {
        TaskRequest request =
                objectMapper.convertValue(
                        context.arguments(),
                        TaskRequest.class
                );
        TaskResponse response =
                taskService.createTask(
                        request,
                        context.user()
                );
        return new ToolResult<>(
                response,
                "Task created successfully."
        );

    }



}
