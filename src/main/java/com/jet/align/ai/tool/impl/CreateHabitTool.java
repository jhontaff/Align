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

@Component
@RequiredArgsConstructor
public class CreateHabitTool implements Tool<HabitResponse> {

    private final HabitService  habitService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Short, clear name of the habit.",
                  "maxLength": 100
                }
              },
              "required": ["name"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "create_habit";
    }

    @Override
    public String description() {
        return "Creates a new habit for the authenticated user.";
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
        HabitRequest request = objectMapper.convertValue(context.arguments(), HabitRequest.class);
        HabitResponse response = habitService.createHabit(context.user(), request);
        return new ToolResult<>(response, "Habit created successfully.");

    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
