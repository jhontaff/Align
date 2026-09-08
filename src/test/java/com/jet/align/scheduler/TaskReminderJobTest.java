package com.jet.align.scheduler;

import com.jet.align.notification.NotificationService;
import com.jet.align.task.Task;
import com.jet.align.task.TaskService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskReminderJobTest {

    private final TaskService taskService = mock(TaskService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TaskReminderJob job = new TaskReminderJob(taskService, notificationService);

    private Task taskOf(User user, String title, LocalTime dueTime) {
        Task task = new Task();
        task.setUser(user);
        task.setTitle(title);
        task.setDueTime(dueTime);
        return task;
    }

    @Test
    void run_sin_tareas_por_recordar_no_notifica() {
        when(taskService.findTasksDueForReminder()).thenReturn(List.of());

        job.run();

        verifyNoInteractions(notificationService);
        verify(taskService, never()).markReminderSent(any());
    }

    @Test
    void run_notifica_y_marca_como_enviada_la_tarea_por_recordar() {
        User user = new User();
        Task task = taskOf(user, "Pagar la luz", LocalTime.of(15, 0));
        when(taskService.findTasksDueForReminder()).thenReturn(List.of(task));

        job.run();

        verify(notificationService).notify(eq(user), anyString(), contains("Pagar la luz"), eq("/tasks"));
        verify(taskService).markReminderSent(task.getId());
    }

    @Test
    void run_procesa_cada_tarea_encontrada() {
        User firstUser = new User();
        User secondUser = new User();
        Task first = taskOf(firstUser, "Pagar la luz", LocalTime.of(15, 0));
        Task second = taskOf(secondUser, "Entregar informe", LocalTime.of(9, 30));
        when(taskService.findTasksDueForReminder()).thenReturn(List.of(first, second));

        job.run();

        verify(notificationService).notify(eq(firstUser), anyString(), contains("Pagar la luz"), eq("/tasks"));
        verify(notificationService).notify(eq(secondUser), anyString(), contains("Entregar informe"), eq("/tasks"));
        // first y second nunca se persistieron, así que sus id son null en los dos --
        // no se pueden distinguir por id acá (mismo detalle que EventReminderJobTest).
        // Los dos verify(notify) de arriba ya prueban que se procesó cada una por
        // separado; esto confirma que markReminderSent se llamó una vez por tarea.
        verify(taskService, times(2)).markReminderSent(any());
        verifyNoMoreInteractions(notificationService);
    }
}
