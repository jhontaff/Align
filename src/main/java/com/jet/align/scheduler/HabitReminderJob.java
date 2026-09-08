package com.jet.align.scheduler;

import com.jet.align.habit.Habit;
import com.jet.align.habit.HabitService;
import com.jet.align.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HabitReminderJob {

    private final HabitService habitService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *", zone = "${align.timezone}")
    public void run() {
        for (Habit habit : habitService.findHabitsDueForReminder()) {
            notificationService.notify(
                    habit.getUser(),
                    "Hora de tu hábito",
                    "Es hora de \"" + habit.getName() + "\".",
                    "/habits");
            habitService.markReminded(habit.getId());
        }
    }
}
