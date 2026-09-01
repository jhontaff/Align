package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteHabitTool implements Tool<Void> {

    private final HabitService habitService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "habitId": {
                  "type": "string",
                  "description": "The unique identifier of the habit to delete, obtained from a prior call to list_habits."
                }
              },
              "required": ["habitId"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "delete_habit";
    }

    @Override
    public String description() {
        return "Deletes a habit and its entire completion history, given its id. If you don't already know the "
                + "habitId, call list_habits first. Call this tool as soon as the user asks to delete a habit -- do not "
                + "ask for permission yourself first. The system will automatically pause the action and require the "
                + "user to confirm it through the app before anything is actually deleted, so calling it immediately "
                + "is safe.";
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
    public ToolResult<Void> execute(ToolContext context) {
        UUID habitId = UUID.fromString((String) context.arguments().get("habitId"));
        habitService.deleteHabit(context.user(), habitId);
        return new ToolResult<>(null, "Habit deleted successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.DESTRUCTIVE;
    }
}
