package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

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
public class UncompleteHabitTool implements Tool<HabitResponse> {

    private final HabitService habitService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "habitId": {
                  "type": "string",
                  "description": "The unique identifier of the habit to undo today's completion for."
                }
              },
              "required": ["habitId"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "uncomplete_habit";
    }

    @Override
    public String description() {
        return "Undoes today's completion of a habit, given its id -- use this when the user marked a habit as done by mistake and wants to revert it back to incomplete. If you only know the habit's name, call list_habits first to find its id.";
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
        HabitResponse response = habitService.uncompleteHabit(context.user(), habitId);
        return new ToolResult<>(response, "Habit marked as incomplete.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }

}
