package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetEventToolTest {

    private final EventService eventService = mock(EventService.class);
    private final GetEventTool tool = new GetEventTool(eventService, new ObjectMapper());
    private final User user = new User();
    private final UUID eventId = UUID.randomUUID();

    @Test
    void execute_convierte_eventId_de_string_a_uuid_y_delega_en_getById() {
        // eventId llega como String crudo, tal como lo arma el LLM a partir del
        // JSON schema -- nunca como un UUID ya parseado.
        EventResponse expected = new EventResponse(eventId, "Reunión con Carlos", null,
                LocalDateTime.of(2026, 9, 5, 15, 0), null, null, null, Instant.now(), Instant.now());
        when(eventService.getById(user, eventId)).thenReturn(expected);

        ToolContext context = new ToolContext(user, Map.of("eventId", eventId.toString()));

        ToolResult<EventResponse> result = tool.execute(context);

        verify(eventService).getById(user, eventId);
        assertThat(result.payload()).isEqualTo(expected);
    }

    @Test
    void get_event_es_una_tool_SAFE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.SAFE);
    }
}
