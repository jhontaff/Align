package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateHabitToolTest {

    private final HabitService habitService = mock(HabitService.class);
    private final UpdateHabitTool tool =
            new UpdateHabitTool(habitService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();
    private final UUID habitId = UUID.randomUUID();

    private HabitResponse existing(String name, LocalTime scheduledTime) {
        return new HabitResponse(habitId, name, scheduledTime, 3, 5, false, Instant.now(), Instant.now());
    }

    // update_habit es un patch: solo los campos presentes en los argumentos
    // cambian. Acá el LLM manda solo "name", así que la scheduledTime actual
    // (07:00) tiene que sobrevivir el merge.
    @Test
    void execute_actualiza_solo_el_name_y_conserva_la_scheduledTime_actual() {
        when(habitService.getHabitById(user, habitId)).thenReturn(existing("Trotar", LocalTime.of(7, 0)));
        when(habitService.updateHabit(eq(user), eq(habitId), any(HabitRequest.class)))
                .thenReturn(existing("Salir a trotar", LocalTime.of(7, 0)));

        ToolContext context = new ToolContext(user, Map.of(
                "habitId", habitId.toString(),
                "name", "Salir a trotar"));

        tool.execute(context);

        ArgumentCaptor<HabitRequest> captor = ArgumentCaptor.forClass(HabitRequest.class);
        verify(habitService).updateHabit(eq(user), eq(habitId), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Salir a trotar");
        assertThat(captor.getValue().scheduledTime()).isEqualTo(LocalTime.of(7, 0));
    }

    // Caso inverso: el LLM manda solo "scheduledTime" (como String "HH:mm"), el
    // name actual se conserva y la hora se parsea a LocalTime.
    @Test
    void execute_actualiza_solo_la_scheduledTime_y_conserva_el_name_actual() {
        when(habitService.getHabitById(user, habitId)).thenReturn(existing("Meditar", null));
        when(habitService.updateHabit(eq(user), eq(habitId), any(HabitRequest.class)))
                .thenReturn(existing("Meditar", LocalTime.of(22, 30)));

        ToolContext context = new ToolContext(user, Map.of(
                "habitId", habitId.toString(),
                "scheduledTime", "22:30"));

        ToolResult<HabitResponse> result = tool.execute(context);

        ArgumentCaptor<HabitRequest> captor = ArgumentCaptor.forClass(HabitRequest.class);
        verify(habitService).updateHabit(eq(user), eq(habitId), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Meditar");
        assertThat(captor.getValue().scheduledTime()).isEqualTo(LocalTime.of(22, 30));
        assertThat(result.payload().scheduledTime()).isEqualTo(LocalTime.of(22, 30));
    }

    @Test
    void execute_propaga_ResourceNotFoundException_si_el_habito_no_existe() {
        when(habitService.getHabitById(user, habitId))
                .thenThrow(new ResourceNotFoundException("Habit not found with id: " + habitId));

        ToolContext context = new ToolContext(user, Map.of(
                "habitId", habitId.toString(),
                "name", "x"));

        assertThatThrownBy(() -> tool.execute(context))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(habitService, org.mockito.Mockito.never()).updateHabit(any(), any(), any());
    }

    // Editar nombre/hora es reversible (se edita de nuevo): SAFE, no DESTRUCTIVE.
    // Guarda de regresión para que nadie lo meta en el gate de PendingAction.
    @Test
    void update_habit_es_una_tool_SAFE() {
        assertThat(tool.risk()).isEqualTo(RiskLevel.SAFE);
    }
}
