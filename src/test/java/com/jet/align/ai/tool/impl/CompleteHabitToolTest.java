package com.jet.align.ai.tool.impl;

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

class CompleteHabitToolTest {

    private final HabitService habitService = mock(HabitService.class);
    private final CompleteHabitTool tool = new CompleteHabitTool(habitService, new ObjectMapper());
    private final User user = new User();
    private final UUID habitId = UUID.randomUUID();

    @Test
    void execute_convierte_habitId_de_string_a_uuid_y_delega_en_completeHabit() {
        HabitResponse expected = new HabitResponse(habitId, "Meditar", 4, 4, true, Instant.now(), Instant.now());
        when(habitService.completeHabit(user, habitId)).thenReturn(expected);

        // habitId llega como String crudo, tal como lo arma el LLM a partir del
        // JSON schema -- nunca como un UUID ya parseado.
        ToolContext context = new ToolContext(user, Map.of("habitId", habitId.toString()));

        ToolResult<HabitResponse> result = tool.execute(context);

        verify(habitService).completeHabit(user, habitId);
        assertThat(result.payload()).isEqualTo(expected);
    }
}
