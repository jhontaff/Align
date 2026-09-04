package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateEventToolTest {

    private final EventService eventService = mock(EventService.class);
    private final UpdateEventTool tool = new UpdateEventTool(
            eventService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();
    private final UUID eventId = UUID.randomUUID();

    private EventResponse currentResponse() {
        return new EventResponse(eventId, "Reunión con Carlos", "Notas de la reunión",
                LocalDateTime.of(2026, 9, 5, 15, 0), LocalDateTime.of(2026, 9, 5, 16, 0),
                "Oficina", 30, Instant.now(), Instant.now());
    }

    // "Mueve la reunión de mañana para las 4": el LLM solo manda eventId + startAt.
    // update_event tiene que leer el estado actual vía getById y mergear -- los
    // campos que no vinieron en el patch tienen que sobrevivir intactos.
    @Test
    void execute_mergea_solo_los_campos_presentes_y_conserva_el_resto_del_estado_actual() {
        when(eventService.getById(user, eventId)).thenReturn(currentResponse());
        EventResponse expected = currentResponse();
        when(eventService.update(eq(user), eq(eventId), any(EventRequest.class))).thenReturn(expected);

        ToolContext context = new ToolContext(user, Map.of(
                "eventId", eventId.toString(),
                "startAt", "2026-09-05T16:00:00"));

        ToolResult<EventResponse> result = tool.execute(context);

        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(eventService).update(eq(user), eq(eventId), captor.capture());
        EventRequest merged = captor.getValue();

        assertThat(merged.title()).isEqualTo("Reunión con Carlos");
        assertThat(merged.description()).isEqualTo("Notas de la reunión");
        assertThat(merged.startAt()).isEqualTo(LocalDateTime.of(2026, 9, 5, 16, 0));
        assertThat(merged.endAt()).isEqualTo(LocalDateTime.of(2026, 9, 5, 16, 0));
        assertThat(merged.location()).isEqualTo("Oficina");
        assertThat(merged.reminderMinutesBefore()).isEqualTo(30);
        assertThat(result.payload()).isEqualTo(expected);
    }

    @Test
    void execute_propaga_ResourceNotFoundException_si_el_evento_no_existe() {
        when(eventService.getById(user, eventId))
                .thenThrow(new ResourceNotFoundException("Event not found with id: " + eventId));

        ToolContext context = new ToolContext(user, Map.of(
                "eventId", eventId.toString(),
                "location", "Sala 2"));

        assertThatThrownBy(() -> tool.execute(context))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_event_es_una_tool_SAFE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.SAFE);
    }
}
