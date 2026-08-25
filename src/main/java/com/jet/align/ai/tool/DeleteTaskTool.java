package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteTaskTool implements Tool<Void> {

    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
          "type": "object",
          "properties": {
            "taskId": {
              "type": "string",
              "description": "The unique identifier of the task to delete, obtained from a prior call to list_tasks."
            }
          },
          "required": ["taskId"],
          "additionalProperties": false
        }
        """;


    @Override
    public String name() {
        return "delete_task";
    }

    @Override
    public String description() {
        return "Deletes a task, given its id. If you don't already know the taskId, call list_tasks first. Call this "
                + "tool as soon as the user asks to delete a task -- do not ask for permission yourself first. The system "
                + "will automatically pause the action and require the user to confirm it through the app before anything "
                + "is actually deleted, so calling it immediately is safe.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(PARAMETERS_SCHEMA, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON Schema for tool " + name(), e);
        }
    }

    public ToolResult<Void> execute(ToolContext context) {
        UUID taskId = UUID.fromString((String) context.arguments().get("taskId"));
        taskService.deleteTask(taskId, context.user());
        return new ToolResult<>(null, "Task deleted successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.DESTRUCTIVE;
    }
}
