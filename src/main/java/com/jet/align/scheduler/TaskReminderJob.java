package com.jet.align.scheduler;

import com.jet.align.notification.NotificationService;
import com.jet.align.task.Task;
import com.jet.align.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TaskReminderJob {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TaskService taskService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *", zone = "${align.timezone}")
    public void run() {
        for (Task task : taskService.findTasksDueForReminder()) {
            notificationService.notify(
                    task.getUser(),
                    "Recordatorio de tarea",
                    "\"" + task.getTitle() + "\"  a las " + task.getDueTime().format(TIME_FORMAT),
                    "/tasks");
            taskService.markReminderSent(task.getId());
        }
    }
}
