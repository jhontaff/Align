package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompleteHabitTool implements Tool<HabitResponse> {

    private final HabitService habitService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "habitId": {
                  "type": "string",
                  "description": "The unique identifier of the habit to mark as completed for today."
                }
              },
              "required": ["habitId"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "complete_habit";
    }

    @Override
    public String description() {
        return "Marks a habit as completed for today, given its id. If you only know the habit's name, call list_habits first to find its id.";
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
    public ToolResult<HabitResponse> execute(ToolContext context) {
        UUID habitId = UUID.fromString((String) context.arguments().get("habitId"));
        HabitResponse response = habitService.completeHabit(context.user(), habitId);
        return new ToolResult<>(response, "Habit marked as complete.");
    }

}
