package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateEventToolTest {

    private final EventService eventService = mock(EventService.class);
    // La app registra jackson-datatype-jsr310 en su ObjectMapper real (ver
    // CreateTransactionToolTest); acá hace falta a mano para parsear startAt/endAt
    // desde un String ISO-8601 como lo manda el LLM.
    private final CreateEventTool tool = new CreateEventTool(
            eventService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();

    @Test
    void execute_convierte_los_argumentos_del_llm_y_delega_en_create() {
        EventResponse expected = new EventResponse(UUID.randomUUID(), "Reunión con Carlos", null,
                LocalDateTime.of(2026, 9, 5, 15, 0), null, null, 30, Instant.now(), Instant.now());
        when(eventService.create(eq(user), any(EventRequest.class))).thenReturn(expected);

        // Los argumentos llegan tal como los arma el LLM a partir del JSON schema:
        // startAt como String crudo, reminderMinutesBefore ya como número.
        ToolContext context = new ToolContext(user, Map.of(
                "title", "Reunión con Carlos",
                "startAt", "2026-09-05T15:00:00",
                "reminderMinutesBefore", 30));

        ToolResult<EventResponse> result = tool.execute(context);

        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(eventService).create(eq(user), captor.capture());
        EventRequest request = captor.getValue();

        assertThat(request.title()).isEqualTo("Reunión con Carlos");
        assertThat(request.startAt()).isEqualTo(LocalDateTime.of(2026, 9, 5, 15, 0));
        assertThat(request.reminderMinutesBefore()).isEqualTo(30);
        assertThat(result.payload()).isEqualTo(expected);
    }

    @Test
    void create_event_es_una_tool_SAFE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.SAFE);
    }
}
