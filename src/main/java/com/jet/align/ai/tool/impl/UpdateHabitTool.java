package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateHabitTool implements Tool<HabitResponse> {

    private final HabitService habitService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "habitId": {
                  "type": "string",
                  "description": "The unique identifier of the habit to rename, obtained from a prior call to list_habits."
                },
                "name": {
                  "type": "string",
                  "description": "The new name for the habit.",
                  "maxLength": 100
                }
              },
              "required": ["habitId", "name"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "update_habit";
    }

    @Override
    public String description() {
        return "Renames an existing habit. This only changes the habit's name; it does not touch its completion "
                + "history or streaks. If you don't already know the habitId, call list_habits first.";
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
        String name = (String) context.arguments().get("name");
        HabitResponse response = habitService.updateHabit(context.user(), habitId, new HabitRequest(name));
        return new ToolResult<>(response, "Habit renamed successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
