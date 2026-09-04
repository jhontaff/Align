package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreateEventTool implements Tool<EventResponse> {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "title":       { "type": "string", "maxLength": 255, "description": "Short title of the event." },
                "description": { "type": "string", "maxLength": 2000, "description": "Optional details." },
                "startAt":     { "type": "string", "format": "date-time", "description": "Start, ISO-8601 local (YYYY-MM-DDTHH:mm:ss)." },
                "endAt":       { "type": "string", "format": "date-time", "description": "Optional end; must be after startAt." },
                "location":    { "type": "string", "maxLength": 255, "description": "Optional free-text location." },
                "reminderMinutesBefore": { "type": "integer", "minimum": 0, "description": "Optional: minutes before startAt to send one reminder." }
              },
              "required": ["title", "startAt"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "create_event";
    }

    @Override
    public String description() {
        return "Creates a calendar event for the authenticated user. Resolve relative expressions like 'mañana a las 3 PM' or 'el viernes' against the current date and time already given in the system prompt — do not ask the user for an exact date. Datetimes are ISO-8601 local.";
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
        EventRequest request = objectMapper.convertValue(context.arguments(), EventRequest.class);
        EventResponse response = eventService.create(context.user(), request);
        return new ToolResult<>(response, "Event created successfully.");
    }


    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
