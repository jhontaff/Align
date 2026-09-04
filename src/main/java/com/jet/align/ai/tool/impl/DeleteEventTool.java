package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.calendar.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteEventTool implements Tool<Void> {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "eventId": {
                  "type": "string",
                  "description": "The unique identifier of the event to delete, obtained from a prior call to list_events."
                }
              },
              "required": ["eventId"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "delete_event";
    }

    @Override
    public String description() {
        return "Deletes an event, given its id. If you don't already know the eventId, call list_events first. Call "
                + "this tool as soon as the user asks to delete or cancel an event -- do not ask for permission "
                + "yourself first. The system will automatically pause the action and require the user to confirm "
                + "it through the app before anything is actually deleted, so calling it immediately is safe.";
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
        UUID eventId = UUID.fromString((String) context.arguments().get("eventId"));
        eventService.delete(context.user(), eventId);
        return new ToolResult<>(null, "Event deleted successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.DESTRUCTIVE;
    }
}
