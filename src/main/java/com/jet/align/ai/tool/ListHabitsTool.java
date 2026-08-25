package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListHabitsTool implements Tool<List<HabitResponse>> {

    private final HabitService  habitService;
    private  final ObjectMapper mapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {},
              "required": [],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "list_habits";
    }

    @Override
    public String description() {
        return "Lists the authenticated user's habits, including their current and longest streak.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return mapper.readValue(PARAMETERS_SCHEMA, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON Schema for tool " + name(), e);
        }
    }

    @Override
    public ToolResult<List<HabitResponse>> execute(ToolContext context) {
        List<HabitResponse> habits = habitService.getHabits(context.user());
        return new ToolResult<>(habits, "Habits retrieved successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }

}
