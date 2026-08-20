package com.jet.align.habit;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HabitControllerTest {

    private final HabitService habitService = mock(HabitService.class);
    private final HabitController controller = new HabitController(habitService);
    private final User user = new User();

    private HabitResponse sampleResponse(UUID id, int currentStreak, int longestStreak) {
        return new HabitResponse(id, "Meditar", currentStreak, longestStreak, Instant.now(), Instant.now());
    }

    @Test
    void createHabit_devuelve_201_con_el_habito_creado_por_el_service() {
        HabitRequest request = new HabitRequest("Meditar");
        HabitResponse expected = sampleResponse(UUID.randomUUID(), 0, 0);
        when(habitService.createHabit(user, request)).thenReturn(expected);

        ResponseEntity<ApiResponse<HabitResponse>> response = controller.createHabit(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void getHabitById_devuelve_200_con_el_habito_del_service() {
        UUID id = UUID.randomUUID();
        HabitResponse expected = sampleResponse(id, 3, 5);
        when(habitService.getHabitById(user, id)).thenReturn(expected);

        ResponseEntity<ApiResponse<HabitResponse>> response = controller.getHabitById(id, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void getHabits_devuelve_200_con_la_lista_del_service() {
        HabitResponse expected = sampleResponse(UUID.randomUUID(), 1, 1);
        when(habitService.getHabits(user)).thenReturn(List.of(expected));

        ResponseEntity<ApiResponse<List<HabitResponse>>> response = controller.getHabits(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).containsExactly(expected);
    }

    @Test
    void updateHabit_devuelve_200_con_el_habito_actualizado() {
        UUID id = UUID.randomUUID();
        HabitRequest request = new HabitRequest("Meditar 10 minutos");
        HabitResponse expected = sampleResponse(id, 2, 2);
        when(habitService.updateHabit(user, id, request)).thenReturn(expected);

        ResponseEntity<ApiResponse<HabitResponse>> response = controller.updateHabit(id, request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void deleteHabit_delega_en_el_service_y_devuelve_200_sin_body() {
        UUID id = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> response = controller.deleteHabit(id, user);

        verify(habitService).deleteHabit(user, id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void completeHabit_devuelve_200_con_el_habito_y_streak_actualizados() {
        UUID id = UUID.randomUUID();
        HabitResponse expected = sampleResponse(id, 4, 5);
        when(habitService.completeHabit(user, id)).thenReturn(expected);

        ResponseEntity<ApiResponse<HabitResponse>> response = controller.completeHabit(id, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }
}
