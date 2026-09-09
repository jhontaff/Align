package com.jet.align.scheduler;

import com.jet.align.habit.Habit;
import com.jet.align.habit.HabitService;
import com.jet.align.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HabitReminderJob {

    private static final Logger log = LoggerFactory.getLogger(HabitReminderJob.class);

    private final HabitService habitService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *", zone = "${align.timezone}")
    public void run() {
        List<Habit> due = habitService.findHabitsDueForReminder();
        if (due.isEmpty()) {
            return;
        }
        log.info("HabitReminder: {} hábito(s) por recordar", due.size());
        for (Habit habit : due) {
            notificationService.notify(
                    habit.getUser(),
                    "Hora de tu hábito",
                    "Es hora de \"" + habit.getName() + "\".",
                    "/habits");
            habitService.markReminded(habit.getId());
        }
    }
}
