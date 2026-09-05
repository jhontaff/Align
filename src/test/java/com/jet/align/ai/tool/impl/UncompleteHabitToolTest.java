package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UncompleteHabitToolTest {

    private final HabitService habitService = mock(HabitService.class);
    private final UncompleteHabitTool tool = new UncompleteHabitTool(habitService, new ObjectMapper());
    private final User user = new User();
    private final UUID habitId = UUID.randomUUID();

    @Test
    void execute_convierte_habitId_de_string_a_uuid_y_delega_en_uncompleteHabit() {
        HabitResponse expected = new HabitResponse(habitId, "Meditar", 0, 4, false, Instant.now(), Instant.now());
        when(habitService.uncompleteHabit(user, habitId)).thenReturn(expected);

        ToolContext context = new ToolContext(user, Map.of("habitId", habitId.toString()));

        ToolResult<HabitResponse> result = tool.execute(context);

        verify(habitService).uncompleteHabit(user, habitId);
        assertThat(result.payload()).isEqualTo(expected);
    }

    @Test
    void risk_es_SAFE_porque_deshacer_una_completion_es_corregible_volviendo_a_completarla() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.SAFE);
    }
}
