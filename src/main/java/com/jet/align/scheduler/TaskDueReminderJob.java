package com.jet.align.scheduler;

import com.jet.align.notification.NotificationService;
import com.jet.align.task.Task;
import com.jet.align.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskDueReminderJob {
    private final TaskService taskService;
    private final NotificationService notificationService;
    @Scheduled(cron = "0 0 18 * * *", zone = "${align.timezone}")
    public void run() {
        for (Task task : taskService.findTasksDueToday()) {
            notificationService.notify(task.getUser(), "Tarea vence hoy",
                    "\"" + task.getTitle() + "\" vence hoy.");
        }
    }
}
