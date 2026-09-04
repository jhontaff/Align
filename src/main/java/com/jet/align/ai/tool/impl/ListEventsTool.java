package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventFilter;
import com.jet.align.calendar.dto.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListEventsTool implements Tool<List<EventResponse>> {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "from": { "type": "string", "format": "date-time", "description": "Inclusive lower bound on startAt." },
                "to":   { "type": "string", "format": "date-time", "description": "Exclusive upper bound on startAt." }
              },
              "required": [],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "list_events";
    }

    @Override
    public String description() {
        return "Lists the authenticated user's events, optionally within a datetime range [from, to). Compute "
                + "from/to yourself from the current date-time already given in the system prompt for requests like "
                + "'hoy', 'mañana', 'esta semana', 'próximos eventos'. Omit both to list all events ordered by start time.";
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
    public ToolResult<List<EventResponse>> execute(ToolContext context) {
        EventFilter filter = objectMapper.convertValue(context.arguments(), EventFilter.class);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "startAt"));
        List<EventResponse> events = eventService.list(context.user(), filter, pageable).getContent();
        return new ToolResult<>(events, "Events retrieved successfully.");
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.SAFE;
    }
}
