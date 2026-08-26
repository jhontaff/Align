package com.jet.align.habit.impl;

import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.habit.*;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HabitServiceImplTest {

    private final HabitRepository habitRepository = mock(HabitRepository.class);
    private final HabitCompletionRepository habitCompletionRepository = mock(HabitCompletionRepository.class);
    private final HabitMapper mapper = mock(HabitMapper.class);
    private final HabitServiceImpl service = new HabitServiceImpl(habitRepository, mapper, habitCompletionRepository, "UTC");
    private final User user = new User();

    private HabitResponse sampleResponse(UUID id, int currentStreak, int longestStreak) {
        return new HabitResponse(id, "Meditar", currentStreak, longestStreak, Instant.now(), Instant.now());
    }

    private HabitCompletion completionOn(LocalDate date) {
        HabitCompletion completion = new HabitCompletion();
        completion.setDate(date);
        return completion;
    }

    @Test
    void al_crear_un_habito_el_streak_inicial_es_siempre_cero() {
        HabitRequest request = new HabitRequest("Meditar");
        Habit mapped = new Habit();
        HabitResponse expected = sampleResponse(UUID.randomUUID(), 0, 0);

        when(mapper.toEntity(request)).thenReturn(mapped);
        when(habitRepository.save(mapped)).thenReturn(mapped);
        when(mapper.toResponse(mapped, 0, 0)).thenReturn(expected);

        HabitResponse response = service.createHabit(user, request);

        assertThat(mapped.getUser()).isEqualTo(user);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void getHabitById_devuelve_el_habito_mapeado_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        HabitResponse expected = sampleResponse(id, 0, 0);

        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of());
        when(mapper.toResponse(habit, 0, 0)).thenReturn(expected);

        HabitResponse response = service.getHabitById(user, id);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void getHabitById_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHabitById(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getHabits_devuelve_cada_habito_del_usuario_con_su_streak_calculado() {
        Habit habit1 = new Habit();
        Habit habit2 = new Habit();
        HabitResponse response1 = sampleResponse(UUID.randomUUID(), 3, 3);
        HabitResponse response2 = sampleResponse(UUID.randomUUID(), 0, 0);
        LocalDate today = LocalDate.now();

        when(habitRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(habit1, habit2));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit1)).thenReturn(List.of(
                completionOn(today), completionOn(today.minusDays(1)), completionOn(today.minusDays(2))));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit2)).thenReturn(List.of());
        when(mapper.toResponse(habit1, 3, 3)).thenReturn(response1);
        when(mapper.toResponse(habit2, 0, 0)).thenReturn(response2);

        List<HabitResponse> responses = service.getHabits(user);

        assertThat(responses).containsExactly(response1, response2);
    }

    @Test
    void updateHabit_actualiza_el_habito_via_mapper_y_recalcula_el_streak() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        HabitRequest request = new HabitRequest("Meditar 10 minutos");
        HabitResponse expected = sampleResponse(id, 2, 2);
        LocalDate today = LocalDate.now();

        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitRepository.save(habit)).thenReturn(habit);
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today), completionOn(today.minusDays(1))));
        when(mapper.toResponse(habit, 2, 2)).thenReturn(expected);

        HabitResponse response = service.updateHabit(user, id, request);

        verify(mapper).updateEntity(request, habit);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void updateHabit_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        HabitRequest request = new HabitRequest("x");
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateHabit(user, id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteHabit_elimina_el_habito_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));

        service.deleteHabit(user, id);

        verify(habitRepository).delete(habit);
    }

    @Test
    void deleteHabit_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteHabit(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(habitRepository, never()).delete(any(Habit.class));
    }

    @Test
    void completeHabit_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeHabit(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeHabit_registra_una_nueva_completion_cuando_no_existe_una_para_hoy() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();

        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.existsByHabitAndDate(habit, today)).thenReturn(false);
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(completionOn(today)));
        when(mapper.toResponse(habit, 1, 1)).thenReturn(sampleResponse(id, 1, 1));

        service.completeHabit(user, id);

        ArgumentCaptor<HabitCompletion> captor = ArgumentCaptor.forClass(HabitCompletion.class);
        verify(habitCompletionRepository).save(captor.capture());
        assertThat(captor.getValue().getHabit()).isEqualTo(habit);
        assertThat(captor.getValue().getDate()).isEqualTo(today);
    }

    // completeHabit es idempotente a propósito: si el LLM reintenta la tool call
    // ("ya lo hice hoy") dos veces el mismo día, la segunda no debe duplicar el
    // registro ni fallar contra el UNIQUE(habit_id, date) de la base.
    @Test
    void completeHabit_es_idempotente_si_el_habito_ya_estaba_completado_hoy() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();

        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.existsByHabitAndDate(habit, today)).thenReturn(true);
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(completionOn(today)));
        when(mapper.toResponse(habit, 1, 1)).thenReturn(sampleResponse(id, 1, 1));

        HabitResponse response = service.completeHabit(user, id);

        verify(habitCompletionRepository, never()).save(any(HabitCompletion.class));
        assertThat(response.currentStreak()).isEqualTo(1);
    }

    @Test
    void streak_es_cero_cuando_no_hay_completions() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of());
        when(mapper.toResponse(habit, 0, 0)).thenReturn(sampleResponse(id, 0, 0));

        HabitResponse response = service.getHabitById(user, id);

        assertThat(response.currentStreak()).isEqualTo(0);
        assertThat(response.longestStreak()).isEqualTo(0);
    }

    @Test
    void streak_cuenta_dias_consecutivos_terminando_hoy() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today), completionOn(today.minusDays(1)), completionOn(today.minusDays(2))));
        when(mapper.toResponse(habit, 3, 3)).thenReturn(sampleResponse(id, 3, 3));

        HabitResponse response = service.getHabitById(user, id);

        assertThat(response.currentStreak()).isEqualTo(3);
    }

    // Día de gracia: si todavía no se marcó hoy pero sí ayer, la racha sigue viva
    // en vez de resetearse a 0 -- el día actual no terminó todavía.
    @Test
    void streak_sigue_vivo_si_la_ultima_completion_fue_ayer() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today.minusDays(1)), completionOn(today.minusDays(2))));
        when(mapper.toResponse(habit, 2, 2)).thenReturn(sampleResponse(id, 2, 2));

        HabitResponse response = service.getHabitById(user, id);

        assertThat(response.currentStreak()).isEqualTo(2);
    }

    // El streak actual se resetea por inactividad, pero el récord histórico no se
    // "olvida" solo porque pasó tiempo -- una única completion aislada sigue siendo
    // una racha de 1 en el historial, aunque hoy la racha vigente sea 0.
    @Test
    void streak_se_resetea_a_cero_si_la_ultima_completion_fue_hace_mas_de_un_dia_pero_el_record_no_se_pierde() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today.minusDays(3))));
        when(mapper.toResponse(habit, 0, 1)).thenReturn(sampleResponse(id, 0, 1));

        HabitResponse response = service.getHabitById(user, id);

        assertThat(response.currentStreak()).isEqualTo(0);
        assertThat(response.longestStreak()).isEqualTo(1);
    }

    @Test
    void streak_se_corta_en_el_primer_hueco_sin_seguir_contando_dias_anteriores() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today), completionOn(today.minusDays(1)), completionOn(today.minusDays(5))));
        when(mapper.toResponse(habit, 2, 2)).thenReturn(sampleResponse(id, 2, 2));

        HabitResponse response = service.getHabitById(user, id);

        assertThat(response.currentStreak()).isEqualTo(2);
    }

    // La racha máxima puede seguir siendo mayor que la actual: acá la racha vigente
    // es de 2 días, pero hace tiempo hubo una racha de 4 que ya se cortó.
    @Test
    void longestStreak_puede_ser_mayor_que_la_racha_actual_por_una_racha_pasada_mas_larga() {
        UUID id = UUID.randomUUID();
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today), completionOn(today.minusDays(1)),
                completionOn(today.minusDays(10)), completionOn(today.minusDays(11)),
                completionOn(today.minusDays(12)), completionOn(today.minusDays(13))));
        when(mapper.toResponse(habit, 2, 4)).thenReturn(sampleResponse(id, 2, 4));

        HabitResponse response = service.getHabitById(user, id);

        assertThat(response.currentStreak()).isEqualTo(2);
        assertThat(response.longestStreak()).isEqualTo(4);
    }

    @Test
    void findHabitsAtRisk_incluye_habitos_no_completados_hoy_con_racha_activa() {
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findAll()).thenReturn(List.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today.minusDays(1)), completionOn(today.minusDays(2))));

        List<Habit> atRisk = service.findHabitsAtRisk();

        assertThat(atRisk).containsExactly(habit);
    }

    @Test
    void findHabitsAtRisk_excluye_habitos_ya_completados_hoy() {
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findAll()).thenReturn(List.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today), completionOn(today.minusDays(1))));

        List<Habit> atRisk = service.findHabitsAtRisk();

        assertThat(atRisk).isEmpty();
    }

    @Test
    void findHabitsAtRisk_excluye_habitos_sin_racha_activa() {
        Habit habit = new Habit();
        LocalDate today = LocalDate.now();
        when(habitRepository.findAll()).thenReturn(List.of(habit));
        when(habitCompletionRepository.findByHabitOrderByDateDesc(habit)).thenReturn(List.of(
                completionOn(today.minusDays(5))));

        List<Habit> atRisk = service.findHabitsAtRisk();

        assertThat(atRisk).isEmpty();
    }

    @Test
    void findHabitsAtRisk_devuelve_vacio_si_no_hay_habitos() {
        when(habitRepository.findAll()).thenReturn(List.of());

        List<Habit> atRisk = service.findHabitsAtRisk();

        assertThat(atRisk).isEmpty();
    }
}
