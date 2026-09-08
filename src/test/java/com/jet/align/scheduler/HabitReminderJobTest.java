package com.jet.align.scheduler;

import com.jet.align.habit.Habit;
import com.jet.align.habit.HabitService;
import com.jet.align.notification.NotificationService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HabitReminderJobTest {

    private final HabitService habitService = mock(HabitService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final HabitReminderJob job = new HabitReminderJob(habitService, notificationService);

    private Habit habitOf(User user, String name) {
        Habit habit = new Habit();
        habit.setUser(user);
        habit.setName(name);
        return habit;
    }

    @Test
    void run_sin_habitos_por_recordar_no_notifica() {
        when(habitService.findHabitsDueForReminder()).thenReturn(List.of());

        job.run();

        verifyNoInteractions(notificationService);
        verify(habitService, never()).markReminded(any());
    }

    @Test
    void run_notifica_y_marca_como_recordado_el_habito() {
        User user = new User();
        Habit habit = habitOf(user, "Meditar");
        when(habitService.findHabitsDueForReminder()).thenReturn(List.of(habit));

        job.run();

        verify(notificationService).notify(eq(user), anyString(), contains("Meditar"), eq("/habits"));
        verify(habitService).markReminded(habit.getId());
    }

    @Test
    void run_procesa_cada_habito_encontrado() {
        User firstUser = new User();
        User secondUser = new User();
        Habit first = habitOf(firstUser, "Meditar");
        Habit second = habitOf(secondUser, "Leer");
        when(habitService.findHabitsDueForReminder()).thenReturn(List.of(first, second));

        job.run();

        verify(notificationService).notify(eq(firstUser), anyString(), contains("Meditar"), eq("/habits"));
        verify(notificationService).notify(eq(secondUser), anyString(), contains("Leer"), eq("/habits"));
        // first y second nunca se persistieron -> sus id son null en ambos lados; se
        // verifica que markReminded se llamó una vez por hábito, no una sola vez para
        // los dos (mismo detalle que EventReminderJobTest / TaskReminderJobTest).
        verify(habitService, times(2)).markReminded(any());
        verifyNoMoreInteractions(notificationService);
    }
}
