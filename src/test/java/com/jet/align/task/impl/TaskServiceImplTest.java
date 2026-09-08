package com.jet.align.task.impl;

import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.task.Task;
import com.jet.align.task.TaskMapper;
import com.jet.align.task.TaskRepository;
import com.jet.align.task.dto.TaskFilter;
import com.jet.align.task.dto.TaskRequest;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.dto.TaskUpdateRequest;
import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceImplTest {

    private final TaskRepository repository = mock(TaskRepository.class);
    private final TaskMapper mapper = mock(TaskMapper.class);
    private final TaskServiceImpl service = new TaskServiceImpl(repository, mapper, "UTC");
    private final User user = new User();

    private TaskResponse sampleResponse(UUID id, TaskStatus status) {
        return new TaskResponse(
                id, "Comprar leche", "Ir al super", status, Priority.MEDIUM,
                LocalDate.of(2026, 8, 25), LocalTime.of(14, 30), Instant.now(), Instant.now());
    }

    // TaskRequest no tiene un campo "status": el service siempre fuerza PENDING al
    // crear, sin importar qué devuelva el mapper -- un cliente no puede crear una
    // tarea ya completada o en progreso.
    @Test
    void al_crear_una_tarea_el_status_siempre_es_pending_y_se_asigna_el_usuario() {
        TaskRequest request = new TaskRequest(
                "Comprar leche", "Ir al super", Priority.MEDIUM,
                LocalDate.of(2026, 8, 25), LocalTime.of(14, 30));
        Task mapped = new Task();
        TaskResponse expected = sampleResponse(UUID.randomUUID(), TaskStatus.PENDING);

        when(mapper.toEntity(request)).thenReturn(mapped);
        when(repository.save(mapped)).thenReturn(mapped);
        when(mapper.toResponse(mapped)).thenReturn(expected);

        TaskResponse response = service.createTask(request, user);

        assertThat(mapped.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(mapped.getUser()).isEqualTo(user);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void getTaskById_devuelve_la_tarea_mapeada_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Task task = new Task();
        TaskResponse expected = sampleResponse(id, TaskStatus.PENDING);
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(task));
        when(mapper.toResponse(task)).thenReturn(expected);

        TaskResponse response = service.getTaskById(id, user);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void getTaskById_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTaskById(id, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // A diferencia de un derived method, una Specification es un lambda: no hay
    // forma de comparar dos instancias por igualdad de contenido, así que el mock
    // se stubea con any(Specification.class) -- lo que se prueba acá es que
    // getTasks delega en findAll(spec, pageable) y mapea cada resultado, no el
    // detalle interno de qué predicados arma TaskSpecifications (eso es trivial y
    // no tiene ramas condicionales que ameriten un test propio). Mismo enfoque que
    // TransactionServiceImplTest.getTransactions_delega_en_el_repository_con_specification_y_pageable_y_mapea_cada_resultado.
    @Test
    void getTasks_sin_filtro_delega_en_findAll_con_specification_y_pageable() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = new Task();
        TaskResponse expected = sampleResponse(UUID.randomUUID(), TaskStatus.PENDING);
        Page<Task> page = new PageImpl<>(List.of(task));

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(task)).thenReturn(expected);

        Page<TaskResponse> response = service.getTasks(user, pageable, new TaskFilter(null, null, null));

        assertThat(response.getContent()).containsExactly(expected);
    }

    @Test
    void getTasks_con_status_y_rango_de_fechas_tambien_delega_en_findAll_con_specification() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = new Task();
        TaskResponse expected = sampleResponse(UUID.randomUUID(), TaskStatus.COMPLETED);
        Page<Task> page = new PageImpl<>(List.of(task));
        TaskFilter filter = new TaskFilter(
                TaskStatus.COMPLETED, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(task)).thenReturn(expected);

        Page<TaskResponse> response = service.getTasks(user, pageable, filter);

        assertThat(response.getContent()).containsExactly(expected);
    }

    @Test
    void updateTask_actualiza_la_tarea_via_mapper_y_devuelve_la_version_guardada() {
        UUID id = UUID.randomUUID();
        Task task = new Task();
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Comprar pan", "Panadería", TaskStatus.IN_PROGRESS, Priority.HIGH,
                LocalDate.of(2026, 8, 26), null);
        TaskResponse expected = sampleResponse(id, TaskStatus.IN_PROGRESS);

        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);
        when(mapper.toResponse(task)).thenReturn(expected);

        TaskResponse response = service.updateTask(id, request, user);

        verify(mapper).updateEntity(request, task);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void updateTask_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        TaskUpdateRequest request = new TaskUpdateRequest(
                "x", "y", TaskStatus.PENDING, Priority.LOW, LocalDate.of(2026, 8, 26), null);
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTask(id, request, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTask_elimina_la_tarea_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Task task = new Task();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(task));

        service.deleteTask(id, user);

        verify(repository).delete(task);
    }

    @Test
    void deleteTask_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTask(id, user))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any(Task.class));
    }

    // El digest de las 18:00 ahora trae SOLO tareas sin hora: las que tienen dueTime
    // reciben su push puntual desde TaskReminderJob y no queremos el duplicado acá.
    @Test
    void findTasksDueToday_delega_con_la_fecha_de_hoy_y_excluye_completadas_y_las_que_tienen_hora() {
        Task task = new Task();
        // Misma zona con la que se construye el service (línea ~41): findTasksDueToday
        // resuelve el día con LocalDate.now(timezone), así que el stub tiene que usar
        // la misma o no matchea cuando la fecha local del JVM != fecha UTC.
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        when(repository.findAllByDueDateAndDueTimeIsNullAndStatusNot(today, TaskStatus.COMPLETED))
                .thenReturn(List.of(task));

        List<Task> dueToday = service.findTasksDueToday();

        assertThat(dueToday).containsExactly(task);
    }

    // Mismo límite que getTasks arriba: TaskSpecifications.expirable(...) es un lambda,
    // así que se stubea con any(Specification.class) -- el mock nunca evalúa el predicado
    // real (vencida por fecha, o vence hoy con hora ya pasada). Lo que este test prueba
    // es la mitad que sí es observable sin una consulta JPA real: que todo lo que el
    // repositorio devuelva se marca EXPIRED y se persiste tal cual. La lógica de fecha/hora
    // del predicado en sí queda sin cobertura unitaria -- gap reconocido, no silencioso.
    @Test
    void expireOverdueTasks_marca_como_expired_y_persiste_todo_lo_que_devuelve_el_repositorio() {
        Task pending = new Task();
        pending.setStatus(TaskStatus.PENDING);
        Task inProgress = new Task();
        inProgress.setStatus(TaskStatus.IN_PROGRESS);
        List<Task> expirable = List.of(pending, inProgress);

        when(repository.findAll(any(Specification.class))).thenReturn(expirable);

        service.expireOverdueTasks();

        assertThat(pending.getStatus()).isEqualTo(TaskStatus.EXPIRED);
        assertThat(inProgress.getStatus()).isEqualTo(TaskStatus.EXPIRED);
        verify(repository).saveAll(expirable);
    }

    @Test
    void expireOverdueTasks_no_hace_nada_si_no_hay_tareas_vencidas() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        service.expireOverdueTasks();

        verify(repository).saveAll(List.of());
    }

    // dueDate es obligatoria: el path REST lo cubre @Valid, pero el path del AI tool
    // (CreateTaskTool -> convertValue -> createTask) saltea bean validation, así que
    // requireDueDate defiende ahí. BusinessException (no IllegalArgumentException)
    // para que AgentServiceImpl.runTool la convierta en un {"error": ...} limpio.
    @Test
    void createTask_lanza_BusinessException_si_falta_la_fecha_de_vencimiento() {
        TaskRequest request = new TaskRequest(
                "Comprar leche", "Ir al super", Priority.MEDIUM, null, null);

        assertThatThrownBy(() -> service.createTask(request, user))
                .isInstanceOf(BusinessException.class);
        verify(mapper, never()).toEntity(any());
    }

    @Test
    void updateTask_lanza_BusinessException_si_falta_la_fecha_de_vencimiento() {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "x", "y", TaskStatus.PENDING, Priority.LOW, null, null);

        assertThatThrownBy(() -> service.updateTask(UUID.randomUUID(), request, user))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).findByIdAndUser(any(), any());
    }

    // Mismo límite que expireOverdueTasks: dueForReminder(...) es un lambda, se stubea
    // con any(Specification.class). El predicado real (dueDate == hoy, dueTime <= ahora,
    // !reminderSent) queda sin cobertura unitaria -- candidato a integration test.
    @Test
    void findTasksDueForReminder_delega_en_findAll_con_specification() {
        Task task = new Task();
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(task));

        assertThat(service.findTasksDueForReminder()).containsExactly(task);
    }

    @Test
    void markReminderSent_marca_la_tarea_como_notificada_y_persiste() {
        UUID id = UUID.randomUUID();
        Task task = new Task();
        when(repository.findById(id)).thenReturn(Optional.of(task));

        service.markReminderSent(id);

        assertThat(task.isReminderSent()).isTrue();
        verify(repository).save(task);
    }

    @Test
    void markReminderSent_lanza_ResourceNotFoundException_si_la_tarea_no_existe() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markReminderSent(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Editar la agenda (dueDate o dueTime) re-arma el recordatorio: si ya se había
    // enviado, vuelve a quedar pendiente para la nueva hora. mapper.updateEntity es
    // mock, así que se simula la mutación con doAnswer para ejercitar la comparación
    // de reArmReminderIfDueChanged.
    @Test
    void updateTask_rearma_el_recordatorio_si_cambia_la_agenda() {
        UUID id = UUID.randomUUID();
        Task task = new Task();
        task.setDueDate(LocalDate.of(2026, 9, 10));
        task.setDueTime(LocalTime.of(14, 0));
        task.setReminderSent(true);
        TaskUpdateRequest request = new TaskUpdateRequest(
                "t", "d", TaskStatus.PENDING, Priority.MEDIUM,
                LocalDate.of(2026, 9, 10), LocalTime.of(9, 0));

        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);
        doAnswer(inv -> {
            Task target = inv.getArgument(1);
            target.setDueTime(LocalTime.of(9, 0));
            return null;
        }).when(mapper).updateEntity(request, task);

        service.updateTask(id, request, user);

        assertThat(task.isReminderSent()).isFalse();
    }

    @Test
    void updateTask_no_rearma_el_recordatorio_si_la_agenda_no_cambia() {
        UUID id = UUID.randomUUID();
        Task task = new Task();
        task.setDueDate(LocalDate.of(2026, 9, 10));
        task.setDueTime(LocalTime.of(14, 0));
        task.setReminderSent(true);
        TaskUpdateRequest request = new TaskUpdateRequest(
                "nuevo titulo", "d", TaskStatus.PENDING, Priority.MEDIUM,
                LocalDate.of(2026, 9, 10), LocalTime.of(14, 0));

        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(task));
        when(repository.save(task)).thenReturn(task);
        // mapper.updateEntity es mock: no muta dueDate/dueTime -> siguen igual

        service.updateTask(id, request, user);

        assertThat(task.isReminderSent()).isTrue();
    }
}
