package com.jet.align.calendar;

import com.jet.align.calendar.dto.EventFilter;
import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EventService {

    EventResponse create(User user, EventRequest request);
    EventResponse getById(User user, UUID id);
    Page<EventResponse> list(User user, EventFilter filter, Pageable pageable);
    EventResponse update(User user, UUID id, EventRequest request);
    void delete(User user, UUID id);

    List<Event> findDueReminders();     // sin User: barrido de sistema
    void markReminderSent(UUID eventId);
}
