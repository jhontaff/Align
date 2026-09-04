package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetEventTool implements Tool<EventResponse> {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "eventId": {
                  "type": "string",
                  "description": "The unique identifier of the event to retrieve, obtained from a prior call to list_events."
                }
              },
              "required": ["eventId"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "get_event";
    }

    @Override
    public String description() {
        return "Retrieves a single event by its id. If you only know it by name or date, call list_events first.";
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
    public ToolResult<EventResponse> execute(ToolContext context) {
        UUID eventId = UUID.fromString((String) context.arguments().get("eventId"));
        EventResponse response = eventService.getById(context.user(), eventId);
        return new ToolResult<>(response, "Event retrieved successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
