package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateEventTool implements Tool<EventResponse> {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "eventId":     { "type": "string", "description": "The unique identifier of the event to update, obtained from a prior call to list_events." },
                "title":       { "type": "string", "maxLength": 255, "description": "Short title of the event." },
                "description": { "type": "string", "maxLength": 2000, "description": "Optional details." },
                "startAt":     { "type": "string", "format": "date-time", "description": "Start, ISO-8601 local (YYYY-MM-DDTHH:mm:ss)." },
                "endAt":       { "type": "string", "format": "date-time", "description": "Optional end; must be after startAt." },
                "location":    { "type": "string", "maxLength": 255, "description": "Optional free-text location." },
                "reminderMinutesBefore": { "type": "integer", "minimum": 0, "description": "Optional: minutes before startAt to send one reminder." }
              },
              "required": ["eventId"],
              "additionalProperties": false
            }
            """;

    // eventId viaja en el mismo Map<String, Object>, pero se lee aparte, abajo en
    // execute(); ignoreUnknown evita que convertValue() explote por ese campo
    // extra que el patch no necesita.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EventPatch(String title, String description, LocalDateTime startAt,
                              LocalDateTime endAt, String location, Integer reminderMinutesBefore) {}

    @Override
    public String name() {
        return "update_event";
    }

    @Override
    public String description() {
        return "Updates an existing event. Call list_events first if you don't know the eventId. Only the fields "
                + "you pass are changed; omitted fields keep their current value (you cannot clear a field to null "
                + "this way). Re-validates that endAt is after startAt.";
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
        EventResponse current = eventService.getById(context.user(), eventId);

        EventPatch patch = objectMapper.convertValue(context.arguments(), EventPatch.class);

        EventRequest merged = new EventRequest(
                patch.title() != null ? patch.title() : current.title(),
                patch.description() != null ? patch.description() : current.description(),
                patch.startAt() != null ? patch.startAt() : current.startAt(),
                patch.endAt() != null ? patch.endAt() : current.endAt(),
                patch.location() != null ? patch.location() : current.location(),
                patch.reminderMinutesBefore() != null ? patch.reminderMinutesBefore() : current.reminderMinutesBefore()
        );
        EventResponse updated = eventService.update(context.user(), eventId, merged);
        return new ToolResult<>(updated, "Event updated successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
