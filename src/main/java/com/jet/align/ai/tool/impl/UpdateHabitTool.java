package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
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
                  "description": "The unique identifier of the habit to update, obtained from a prior call to list_habits."
                },
                "name": {
                  "type": "string",
                  "description": "The new name for the habit. Omit to leave it unchanged.",
                  "maxLength": 100
                },
                "scheduledTime": {
                  "type": "string",
                  "format": "time",
                  "description": "The time of day the user plans to do the habit, in HH:mm (24-hour). Omit to leave it unchanged."
                }
              },
              "required": ["habitId"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "update_habit";
    }

    @Override
    public String description() {
        return "Updates an existing habit's name and/or the time of day the user plans to do it. It does not touch "
                + "the habit's completion history or streaks. Only the fields you provide are changed; omitted fields "
                + "keep their current value. If you don't already know the habitId, call list_habits first.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(PARAMETERS_SCHEMA, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON Schema for tool " + name(), e);
        }
    }

    // habitId viaja en el mismo Map pero se lee aparte; ignoreUnknown evita que
    // convertValue() explote por ese campo extra que el patch no necesita.
    // scheduledTime null significa "no lo toques", igual que en UpdateTaskTool:
    // no se puede volver a null una hora ya puesta desde el chat (misma limitación
    // documentada para update_task).
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record HabitPatch(String name, LocalTime scheduledTime) {}

    @Override
    public ToolResult<HabitResponse> execute(ToolContext context) {
        UUID habitId = UUID.fromString((String) context.arguments().get("habitId"));
        HabitResponse current = habitService.getHabitById(context.user(), habitId);

        HabitPatch patch = objectMapper.convertValue(context.arguments(), HabitPatch.class);

        HabitRequest merged = new HabitRequest(
                patch.name() != null ? patch.name() : current.name(),
                patch.scheduledTime() != null ? patch.scheduledTime() : current.scheduledTime()
        );
        HabitResponse response = habitService.updateHabit(context.user(), habitId, merged);
        return new ToolResult<>(response, "Habit updated successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
