package com.jet.align.scheduler;

import com.jet.align.calendar.Event;
import com.jet.align.calendar.EventService;
import com.jet.align.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class EventReminderJob {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final EventService eventService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *", zone = "${align.timezone}")
    public void run() {
        for (Event event : eventService.findDueReminders()) {
            notificationService.notify(
                    event.getUser(),
                    "Recordatorio",
                    "\"" + event.getTitle() + "\" comienza a las " + event.getStartAt().format(TIME_FORMAT),
                    "/calendar");
            eventService.markReminderSent(event.getId());
        }
    }

}
