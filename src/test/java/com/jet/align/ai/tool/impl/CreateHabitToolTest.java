package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateHabitToolTest {

    private final HabitService habitService = mock(HabitService.class);
    // JavaTimeModule: la app real lo trae vía Spring Boot autoconfig; acá hace
    // falta para que convertValue() parsee scheduledTime ("HH:mm") a LocalTime.
    private final CreateHabitTool tool =
            new CreateHabitTool(habitService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();

    @Test
    void execute_convierte_los_argumentos_crudos_del_llm_y_delega_en_createHabit() {
        HabitResponse expected = new HabitResponse(
                UUID.randomUUID(), "Meditar", null, 0, 0, false, Instant.now(), Instant.now());
        when(habitService.createHabit(eq(user), any(HabitRequest.class))).thenReturn(expected);

        // "name" llega tal como lo arma el LLM a partir del JSON schema: un String
        // crudo, la única propiedad requerida de HabitRequest.
        ToolContext context = new ToolContext(user, Map.of("name", "Meditar"));

        ToolResult<HabitResponse> result = tool.execute(context);

        ArgumentCaptor<HabitRequest> captor = ArgumentCaptor.forClass(HabitRequest.class);
        verify(habitService).createHabit(eq(user), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Meditar");
        assertThat(captor.getValue().scheduledTime()).isNull();
        assertThat(result.payload()).isEqualTo(expected);
    }

    // scheduledTime llega como String "HH:mm" del schema y tiene que convertirse
    // a LocalTime dentro de HabitRequest.
    @Test
    void execute_convierte_scheduledTime_recibido_como_string_a_LocalTime() {
        when(habitService.createHabit(eq(user), any(HabitRequest.class))).thenReturn(
                new HabitResponse(UUID.randomUUID(), "Meditar", LocalTime.of(7, 30), 0, 0, false,
                        Instant.now(), Instant.now()));

        ToolContext context = new ToolContext(user, Map.of("name", "Meditar", "scheduledTime", "07:30"));

        tool.execute(context);

        ArgumentCaptor<HabitRequest> captor = ArgumentCaptor.forClass(HabitRequest.class);
        verify(habitService).createHabit(eq(user), captor.capture());
        assertThat(captor.getValue().scheduledTime()).isEqualTo(LocalTime.of(7, 30));
    }
}
