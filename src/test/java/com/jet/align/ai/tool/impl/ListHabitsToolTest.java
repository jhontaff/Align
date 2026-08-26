package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListHabitsToolTest {

    private final HabitService habitService = mock(HabitService.class);
    private final ListHabitsTool tool = new ListHabitsTool(habitService, new ObjectMapper());
    private final User user = new User();

    @Test
    void execute_delega_en_getHabits_y_devuelve_la_lista_tal_cual() {
        List<HabitResponse> habits = List.of(
                new HabitResponse(UUID.randomUUID(), "Meditar", 3, 5, Instant.now(), Instant.now()),
                new HabitResponse(UUID.randomUUID(), "Ejercicio", 0, 2, Instant.now(), Instant.now()));
        when(habitService.getHabits(user)).thenReturn(habits);

        // list_habits no toma argumentos -- el schema no declara properties, así
        // que el LLM manda un Map vacío.
        ToolResult<List<HabitResponse>> result = tool.execute(new ToolContext(user, Map.of()));

        assertThat(result.payload()).containsExactlyElementsOf(habits);
    }
}
