package com.jet.align.scheduler;

import com.jet.align.habit.Habit;
import com.jet.align.habit.HabitService;
import com.jet.align.notification.NotificationService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HabitAtRiskJobTest {

    private final HabitService habitService = mock(HabitService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final HabitAtRiskJob job = new HabitAtRiskJob(habitService, notificationService);

    private Habit habitOf(User user, String name) {
        Habit habit = new Habit();
        habit.setUser(user);
        habit.setName(name);
        return habit;
    }

    @Test
    void run_sin_habitos_en_riesgo_no_notifica() {
        when(habitService.findHabitsAtRisk()).thenReturn(List.of());

        job.run();

        verifyNoInteractions(notificationService);
    }

    @Test
    void run_notifica_al_usuario_de_cada_habito_en_riesgo() {
        User user = new User();
        Habit habit = habitOf(user, "Leer");
        when(habitService.findHabitsAtRisk()).thenReturn(List.of(habit));

        job.run();

        verify(notificationService).notify(eq(user), anyString(), contains("Leer"), eq("/habits"));
    }

    @Test
    void run_notifica_un_habito_en_riesgo_por_cada_uno_encontrado() {
        User firstUser = new User();
        User secondUser = new User();
        Habit first = habitOf(firstUser, "Leer");
        Habit second = habitOf(secondUser, "Meditar");
        when(habitService.findHabitsAtRisk()).thenReturn(List.of(first, second));

        job.run();

        verify(notificationService).notify(eq(firstUser), anyString(), contains("Leer"), eq("/habits"));
        verify(notificationService).notify(eq(secondUser), anyString(), contains("Meditar"), eq("/habits"));
        verifyNoMoreInteractions(notificationService);
    }
}
