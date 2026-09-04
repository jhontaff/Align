package com.jet.align.scheduler;

import com.jet.align.calendar.Event;
import com.jet.align.calendar.EventService;
import com.jet.align.notification.NotificationService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventReminderJobTest {

    private final EventService eventService = mock(EventService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final EventReminderJob job = new EventReminderJob(eventService, notificationService);

    private Event eventOf(User user, String title, LocalDateTime startAt) {
        Event event = new Event();
        event.setUser(user);
        event.setTitle(title);
        event.setStartAt(startAt);
        return event;
    }

    @Test
    void run_sin_recordatorios_vencidos_no_notifica() {
        when(eventService.findDueReminders()).thenReturn(List.of());

        job.run();

        verifyNoInteractions(notificationService);
        verify(eventService, never()).markReminderSent(any());
    }

    @Test
    void run_notifica_y_marca_como_enviado_el_recordatorio_vencido() {
        User user = new User();
        Event event = eventOf(user, "Reunión con Carlos", LocalDateTime.of(2026, 9, 10, 15, 0));
        when(eventService.findDueReminders()).thenReturn(List.of(event));

        job.run();

        verify(notificationService).notify(eq(user), anyString(), contains("Reunión con Carlos"), eq("/calendar"));
        verify(eventService).markReminderSent(event.getId());
    }

    @Test
    void run_notifica_y_marca_cada_evento_encontrado() {
        User firstUser = new User();
        User secondUser = new User();
        Event first = eventOf(firstUser, "Reunión con Carlos", LocalDateTime.of(2026, 9, 10, 15, 0));
        Event second = eventOf(secondUser, "Dentista", LocalDateTime.of(2026, 9, 10, 9, 30));
        when(eventService.findDueReminders()).thenReturn(List.of(first, second));

        job.run();

        verify(notificationService).notify(eq(firstUser), anyString(), contains("Reunión con Carlos"), eq("/calendar"));
        verify(notificationService).notify(eq(secondUser), anyString(), contains("Dentista"), eq("/calendar"));
        // first y second nunca se persistieron, así que sus id son null en los dos --
        // no se pueden distinguir por id acá. Lo que prueba que se procesó cada uno
        // por separado ya lo cubren los dos verify(notify) de arriba (usuario+título
        // distintos); esto solo confirma que markReminderSent se llamó una vez por
        // evento encontrado, no una sola vez para los dos.
        verify(eventService, times(2)).markReminderSent(any());
        verifyNoMoreInteractions(notificationService);
    }
}
