package com.jet.align.scheduler;

import com.jet.align.notification.NotificationService;
import com.jet.align.task.Task;
import com.jet.align.task.TaskService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskDueReminderJobTest {

    private final TaskService taskService = mock(TaskService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TaskDueReminderJob job = new TaskDueReminderJob(taskService, notificationService);

    private Task taskOf(User user, String title) {
        Task task = new Task();
        task.setUser(user);
        task.setTitle(title);
        return task;
    }

    @Test
    void run_sin_tareas_por_vencer_no_notifica() {
        when(taskService.findTasksDueToday()).thenReturn(List.of());

        job.run();

        verifyNoInteractions(notificationService);
    }

    @Test
    void run_notifica_al_usuario_de_cada_tarea_que_vence_hoy() {
        User user = new User();
        Task task = taskOf(user, "Pagar la luz");
        when(taskService.findTasksDueToday()).thenReturn(List.of(task));

        job.run();

        verify(notificationService).notify(eq(user), anyString(), contains("Pagar la luz"));
    }

    @Test
    void run_notifica_una_tarea_por_cada_una_encontrada() {
        User firstUser = new User();
        User secondUser = new User();
        Task first = taskOf(firstUser, "Pagar la luz");
        Task second = taskOf(secondUser, "Entregar informe");
        when(taskService.findTasksDueToday()).thenReturn(List.of(first, second));

        job.run();

        verify(notificationService).notify(eq(firstUser), anyString(), contains("Pagar la luz"));
        verify(notificationService).notify(eq(secondUser), anyString(), contains("Entregar informe"));
        verifyNoMoreInteractions(notificationService);
    }
}
