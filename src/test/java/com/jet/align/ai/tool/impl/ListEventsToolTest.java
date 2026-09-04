package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventFilter;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListEventsToolTest {

    private final EventService eventService = mock(EventService.class);
    private final ListEventsTool tool = new ListEventsTool(
            eventService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();

    @Test
    void execute_convierte_from_to_a_EventFilter_y_usa_paginado_fijo_size20_startAt_asc() {
        EventResponse response = new EventResponse(UUID.randomUUID(), "Reunión", null,
                LocalDateTime.of(2026, 9, 5, 15, 0), null, null, null, Instant.now(), Instant.now());
        when(eventService.list(eq(user), any(EventFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        // from/to llegan como Strings ISO-8601 crudos, tal como los arma el LLM.
        ToolContext context = new ToolContext(user, Map.of(
                "from", "2026-09-01T00:00:00",
                "to", "2026-09-08T00:00:00"));

        ToolResult<List<EventResponse>> result = tool.execute(context);

        ArgumentCaptor<EventFilter> filterCaptor = ArgumentCaptor.forClass(EventFilter.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventService).list(eq(user), filterCaptor.capture(), pageableCaptor.capture());

        assertThat(filterCaptor.getValue().from()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
        assertThat(filterCaptor.getValue().to()).isEqualTo(LocalDateTime.of(2026, 9, 8, 0, 0));
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "startAt"));
        assertThat(result.payload()).containsExactly(response);
    }

    // El schema no exige ni from ni to -- "listame mis eventos" sin rango debe
    // resolver en un EventFilter completamente vacío, no fallar.
    @Test
    void execute_sin_argumentos_usa_un_filtro_sin_rango() {
        when(eventService.list(eq(user), any(EventFilter.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        tool.execute(new ToolContext(user, Map.of()));

        ArgumentCaptor<EventFilter> filterCaptor = ArgumentCaptor.forClass(EventFilter.class);
        verify(eventService).list(eq(user), filterCaptor.capture(), any(Pageable.class));
        assertThat(filterCaptor.getValue().from()).isNull();
        assertThat(filterCaptor.getValue().to()).isNull();
    }

    @Test
    void list_events_es_una_tool_SAFE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.SAFE);
    }
}
