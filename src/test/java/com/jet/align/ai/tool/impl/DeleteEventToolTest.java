package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.calendar.EventService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeleteEventToolTest {

    private final EventService eventService = mock(EventService.class);
    private final DeleteEventTool tool = new DeleteEventTool(eventService, new ObjectMapper());
    private final User user = new User();
    private final UUID eventId = UUID.randomUUID();

    @Test
    void execute_convierte_eventId_de_string_a_uuid_y_delega_en_delete() {
        // eventId llega como String crudo, tal como lo arma el LLM a partir del
        // JSON schema -- nunca como un UUID ya parseado.
        ToolContext context = new ToolContext(user, Map.of("eventId", eventId.toString()));

        ToolResult<Void> result = tool.execute(context);

        verify(eventService).delete(user, eventId);
        // Tool<Void>: no hay payload, solo el mensaje para el LLM.
        assertThat(result.payload()).isNull();
    }

    // Borrar un evento destruye su estado sin posibilidad de reconstrucción:
    // DESTRUCTIVE. Bloquea que alguien lo baje a SAFE sin querer y se salte el
    // gate de PendingAction.
    @Test
    void delete_event_es_una_tool_DESTRUCTIVE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.DESTRUCTIVE);
    }
}
