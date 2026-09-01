package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeleteHabitToolTest {

    private final HabitService habitService = mock(HabitService.class);
    private final DeleteHabitTool tool = new DeleteHabitTool(habitService, new ObjectMapper());
    private final User user = new User();
    private final UUID habitId = UUID.randomUUID();

    @Test
    void execute_convierte_habitId_de_string_a_uuid_y_delega_en_deleteHabit() {
        // habitId llega como String crudo, tal como lo arma el LLM a partir del
        // JSON schema -- nunca como un UUID ya parseado.
        ToolContext context = new ToolContext(user, Map.of("habitId", habitId.toString()));

        ToolResult<Void> result = tool.execute(context);

        verify(habitService).deleteHabit(user, habitId);
        // Tool<Void>: no hay payload, solo el mensaje para el LLM.
        assertThat(result.payload()).isNull();
    }

    // delete_habit destruye todo el historial de HabitCompletion del hábito: estado
    // irreconstruible, DESTRUCTIVE. Este test bloquea que alguien lo baje a SAFE y se
    // salte el gate de PendingAction.
    @Test
    void delete_habit_es_una_tool_DESTRUCTIVE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.DESTRUCTIVE);
    }
}
