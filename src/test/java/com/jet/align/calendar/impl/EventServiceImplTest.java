package com.jet.align.calendar.impl;

import com.jet.align.calendar.Event;
import com.jet.align.calendar.EventMapper;
import com.jet.align.calendar.EventRepository;
import com.jet.align.calendar.dto.EventFilter;
import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceImplTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventMapper mapper = mock(EventMapper.class);
    private final EventServiceImpl service = new EventServiceImpl(eventRepository, mapper, "UTC");
    private final User user = new User();

    @BeforeEach
    void setUp() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EventRequest request(LocalDateTime startAt, LocalDateTime endAt, Integer reminderMinutesBefore) {
        return new EventRequest("Reunión con Carlos", null, startAt, endAt, null, reminderMinutesBefore);
    }

    private Event entityFrom(EventRequest request) {
        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setLocation(request.location());
        event.setReminderMinutesBefore(request.reminderMinutesBefore());
        return event;
    }

    // El mapper está mockeado; updateEntity es void y por defecto no hace nada.
    // Este stub replica lo que haría MapStruct: copiar el request sobre la entidad.
    private void stubUpdateEntityCopiesRequestIntoEntity() {
        doAnswer(invocation -> {
            EventRequest r = invocation.getArgument(0);
            Event e = invocation.getArgument(1);
            e.setTitle(r.title());
            e.setDescription(r.description());
            e.setStartAt(r.startAt());
            e.setEndAt(r.endAt());
            e.setLocation(r.location());
            e.setReminderMinutesBefore(r.reminderMinutesBefore());
            return null;
        }).when(mapper).updateEntity(any(EventRequest.class), any(Event.class));
    }

    private EventResponse sampleResponse(UUID id) {
        return new EventResponse(id, "Reunión con Carlos", null,
                LocalDateTime.now(UTC), null, null, null, Instant.now(), Instant.now());
    }

    // --- create -------------------------------------------------------------

    @Test
    void crear_evento_valido_asigna_el_usuario_y_persiste() {
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(1);
        EventRequest req = request(start, null, null);
        Event mapped = entityFrom(req);
        EventResponse expected = sampleResponse(UUID.randomUUID());

        when(mapper.toEntity(req)).thenReturn(mapped);
        when(mapper.toResponse(mapped)).thenReturn(expected);

        EventResponse result = service.create(user, req);

        assertThat(mapped.getUser()).isEqualTo(user);
        assertThat(result).isEqualTo(expected);
        verify(eventRepository).save(mapped);
    }

    @Test
    void crear_evento_con_endAt_igual_a_startAt_lanza_BusinessException() {
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(1);

        assertThatThrownBy(() -> service.create(user, request(start, start, null)))
                .isInstanceOf(BusinessException.class);

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void crear_evento_con_endAt_anterior_a_startAt_lanza_BusinessException() {
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(1);

        assertThatThrownBy(() -> service.create(user, request(start, start.minusHours(1), null)))
                .isInstanceOf(BusinessException.class);

        verify(eventRepository, never()).save(any(Event.class));
    }

    // Vía REST @NotNull lo cubre, pero las AI tools no corren bean-validation:
    // el servicio tiene que rechazar startAt nulo con un mensaje recuperable.
    @Test
    void crear_evento_sin_startAt_lanza_BusinessException() {
        assertThatThrownBy(() -> service.create(user, request(null, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(mapper, never()).toEntity(any());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void crear_con_reminder_calcula_reminderAt_y_deja_reminderSent_false() {
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(1);
        EventRequest req = request(start, null, 30);
        when(mapper.toEntity(req)).thenReturn(entityFrom(req));

        service.create(user, req);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getReminderAt()).isEqualTo(start.minusMinutes(30));
        assertThat(captor.getValue().isReminderSent()).isFalse();
    }

    @Test
    void crear_sin_reminder_deja_reminderAt_null() {
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(1);
        EventRequest req = request(start, null, null);
        when(mapper.toEntity(req)).thenReturn(entityFrom(req));

        service.create(user, req);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getReminderAt()).isNull();
    }

    // --- getById -----------------------------------------------------------

    @Test
    void getById_devuelve_el_evento_mapeado_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Event event = new Event();
        EventResponse expected = sampleResponse(id);

        when(eventRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(event));
        when(mapper.toResponse(event)).thenReturn(expected);

        assertThat(service.getById(user, id)).isEqualTo(expected);
    }

    @Test
    void getById_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- update ----------------------------------------------------------

    @Test
    void update_al_mover_startAt_recalcula_reminderAt_y_resetea_reminderSent() {
        UUID id = UUID.randomUUID();
        LocalDateTime oldStart = LocalDateTime.now(UTC).plusDays(2);
        LocalDateTime newStart = oldStart.plusHours(3);

        Event existing = new Event();
        existing.setStartAt(oldStart);
        existing.setReminderMinutesBefore(15);
        existing.setReminderAt(oldStart.minusMinutes(15));
        existing.setReminderSent(true);

        when(eventRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(existing));
        stubUpdateEntityCopiesRequestIntoEntity();

        service.update(user, id, request(newStart, null, 15));

        assertThat(existing.getReminderAt()).isEqualTo(newStart.minusMinutes(15));
        assertThat(existing.isReminderSent()).isFalse();
    }

    @Test
    void update_con_reminderMinutesBefore_null_limpia_reminderAt() {
        UUID id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(2);

        Event existing = new Event();
        existing.setStartAt(start);
        existing.setReminderMinutesBefore(60);
        existing.setReminderAt(start.minusMinutes(60));
        existing.setReminderSent(true);

        when(eventRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(existing));
        stubUpdateEntityCopiesRequestIntoEntity();

        service.update(user, id, request(start, null, null));

        assertThat(existing.getReminderAt()).isNull();
        assertThat(existing.isReminderSent()).isFalse();
    }

    @Test
    void update_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(1);
        when(eventRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(user, id, request(start, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_con_endAt_anterior_a_startAt_lanza_BusinessException() {
        UUID id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now(UTC).plusDays(1);

        assertThatThrownBy(() -> service.update(user, id, request(start, start.minusMinutes(1), null)))
                .isInstanceOf(BusinessException.class);

        verify(eventRepository, never()).findByIdAndUser(any(), any());
    }

    // --- delete ----------------------------------------------------------

    @Test
    void delete_elimina_el_evento_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Event event = new Event();
        when(eventRepository.findByIdAndUser(id, user)).thenReturn(Optional.of(event));

        service.delete(user, id);

        verify(eventRepository).delete(event);
    }

    @Test
    void delete_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).delete(any(Event.class));
    }

    // --- findDueReminders ----------------------------------------------

    @Test
    void findDueReminders_incluye_recordatorio_vencido_de_evento_futuro_y_excluye_eventos_ya_empezados() {
        LocalDateTime now = LocalDateTime.now(UTC);

        Event dueUpcoming = new Event();
        dueUpcoming.setStartAt(now.plusHours(1));
        dueUpcoming.setReminderAt(now.minusMinutes(1));
        dueUpcoming.setReminderSent(false);

        Event alreadyStarted = new Event();
        alreadyStarted.setStartAt(now.minusHours(1));
        alreadyStarted.setReminderAt(now.minusMinutes(30));
        alreadyStarted.setReminderSent(false);

        when(eventRepository.findByReminderSentFalseAndReminderAtLessThanEqual(any()))
                .thenReturn(List.of(dueUpcoming, alreadyStarted));

        assertThat(service.findDueReminders()).containsExactly(dueUpcoming);
    }

    @Test
    void findDueReminders_devuelve_vacio_si_la_consulta_no_trae_nada() {
        when(eventRepository.findByReminderSentFalseAndReminderAtLessThanEqual(any()))
                .thenReturn(List.of());

        assertThat(service.findDueReminders()).isEmpty();
    }

    // --- markReminderSent --------------------------------------------

    @Test
    void markReminderSent_marca_el_evento_y_lo_persiste() {
        UUID id = UUID.randomUUID();
        Event event = new Event();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        service.markReminderSent(id);

        assertThat(event.isReminderSent()).isTrue();
        verify(eventRepository).save(event);
    }

    @Test
    void markReminderSent_lanza_ResourceNotFoundException_si_el_evento_no_existe() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markReminderSent(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- list -----------------------------------------------------------

    @Test
    void list_sin_filtro_usa_la_consulta_sin_rango() {
        Pageable pageable = PageRequest.of(0, 20);
        when(eventRepository.findByUserOrderByStartAtAsc(user, pageable)).thenReturn(Page.empty());

        service.list(user, new EventFilter(null, null), pageable);

        verify(eventRepository).findByUserOrderByStartAtAsc(user, pageable);
        verify(eventRepository, never())
                .findByUserAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(any(), any(), any(), any());
    }

    @Test
    void list_con_rango_usa_la_consulta_por_rango() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime from = LocalDateTime.now(UTC);
        LocalDateTime to = from.plusDays(7);
        when(eventRepository.findByUserAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                user, from, to, pageable)).thenReturn(Page.empty());

        service.list(user, new EventFilter(from, to), pageable);

        verify(eventRepository)
                .findByUserAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(user, from, to, pageable);
        verify(eventRepository, never()).findByUserOrderByStartAtAsc(any(), any());
    }
}
