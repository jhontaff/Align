package com.jet.align.scheduler;

import com.jet.align.habit.Habit;
import com.jet.align.habit.HabitService;
import com.jet.align.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HabitAtRiskJob {
    private final HabitService habitService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 20 * * *", zone = "${align.timezone}")
    public void run() {
        for (Habit habit : habitService.findHabitsAtRisk()) {
            notificationService.notify(habit.getUser(), "Tu racha está en riesgo",
                    "No completaste \"" + habit.getName() + "\" hoy. ¡No pierdas tu racha!", "/habits");
        }
    }
}
