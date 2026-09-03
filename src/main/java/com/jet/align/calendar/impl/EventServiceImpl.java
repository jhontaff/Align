package com.jet.align.calendar.impl;

import com.jet.align.calendar.Event;
import com.jet.align.calendar.EventMapper;
import com.jet.align.calendar.EventRepository;
import com.jet.align.calendar.EventService;
import com.jet.align.calendar.dto.EventFilter;
import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    EventMapper mapper;
    private static final String EVENT_NOT_FOUND_MESSAGE = "Event not found with id: ";
    private final ZoneId timezone;


    public EventServiceImpl(EventRepository eventRepository, EventMapper mapper,
                            @Value("${align.timezone}") String timezone) {
        this.eventRepository = eventRepository;
        this.mapper = mapper;
        this.timezone = ZoneId.of(timezone);
    }

    @Override
    @Transactional
    public EventResponse create(User user, EventRequest request) {
        validate(request);
        Event event = mapper.toEntity(request);
        event.setUser(user);
        applyReminder(event);
        return mapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(User user, UUID id) {
        return mapper.toResponse(findOwnedEvent(user, id));
    }

    @Override
    public Page<EventResponse> list(User user, EventFilter filter, Pageable pageable) {
        LocalDateTime from = filter.from();
        LocalDateTime to = filter.to();
        if(from == null && to == null) {
            return eventRepository.findByUserOrderByStartAtAsc(user, pageable)
                    .map(mapper::toResponse);
        }
        LocalDateTime rangeStart = from != null ? from : LocalDateTime.now(timezone).minusYears(100);
        LocalDateTime rangeEnd = to != null ? to : LocalDateTime.now(timezone).plusYears(100);
        return eventRepository.findByUserAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(user, rangeStart, rangeEnd, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public EventResponse update(User user, UUID id, EventRequest request) {
        validate(request);
        Event event = findOwnedEvent(user, id);
        mapper.updateEntity(request, event);
        applyReminder(event);
        return mapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public void delete(User user, UUID id) {
        Event event = findOwnedEvent(user, id);
        eventRepository.delete(event);
    }

    @Override
    public List<Event> findDueReminders() {
        LocalDateTime now = LocalDateTime.now(timezone);
        return eventRepository.findByReminderSentFalseAndReminderAtLessThanEqual(now).
                stream().filter( event -> event.getStartAt().isAfter(now)).toList();
    }

    @Override
    @Transactional
    public void markReminderSent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MESSAGE + eventId));
        event.setReminderSent(true);
        eventRepository.save(event);
    }

    private Event findOwnedEvent(User user, UUID id) {
        return eventRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MESSAGE + id));
    }

    private void validate(EventRequest request) {
        if (request.startAt()==null) {
            throw new BusinessException("Start time is required");
        }
        if(request.endAt() != null && !request.endAt().isAfter(request.startAt())) {
            throw new BusinessException("End time must be after start time");
        }
    }

    private void applyReminder(Event event) {
        if (event.getReminderMinutesBefore() == null) {
            event.setReminderAt(null);
            event.setReminderSent(false);
            return;
        }
        LocalDateTime next = event.getStartAt().minusMinutes(event.getReminderMinutesBefore());
        if(!next.equals(event.getReminderAt())) {
            event.setReminderAt(next);
            event.setReminderSent(false);
        }
    }

}
