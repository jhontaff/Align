package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateHabitToolTest {

    private final HabitService habitService = mock(HabitService.class);
    private final UpdateHabitTool tool = new UpdateHabitTool(habitService, new ObjectMapper());
    private final User user = new User();
    private final UUID habitId = UUID.randomUUID();

    @Test
    void execute_extrae_habitId_y_name_de_los_argumentos_y_delega_en_updateHabit() {
        HabitResponse expected = new HabitResponse(habitId, "Salir a trotar", 3, 5, false, Instant.now(), Instant.now());
        when(habitService.updateHabit(eq(user), eq(habitId), any(HabitRequest.class))).thenReturn(expected);

        // habitId y name llegan como Strings crudos, tal como los arma el LLM a
        // partir del JSON schema. habitId NO es campo de HabitRequest, por eso se
        // extrae aparte en vez de convertir el Map entero con convertValue.
        ToolContext context = new ToolContext(user, Map.of(
                "habitId", habitId.toString(),
                "name", "Salir a trotar"));

        ToolResult<HabitResponse> result = tool.execute(context);

        ArgumentCaptor<HabitRequest> captor = ArgumentCaptor.forClass(HabitRequest.class);
        verify(habitService).updateHabit(eq(user), eq(habitId), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Salir a trotar");
        assertThat(result.payload()).isEqualTo(expected);
    }

    // Renombrar es reversible (se renombra de nuevo): SAFE, no DESTRUCTIVE. El nombre
    // viejo no es historial como el content de update_memory. Guarda de regresion
    // para que nadie lo meta en el gate de PendingAction sin motivo.
    @Test
    void update_habit_es_una_tool_SAFE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.SAFE);
    }
}
