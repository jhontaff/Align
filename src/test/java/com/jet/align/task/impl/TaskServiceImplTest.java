package com.jet.align.task.impl;

import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.task.Task;
import com.jet.align.task.TaskMapper;
import com.jet.align.task.TaskRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void getTasks_sin_status_delega_en_findAllByUser() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = new Task();
        TaskResponse expected = sampleResponse(UUID.randomUUID(), TaskStatus.PENDING);
        Page<Task> page = new PageImpl<>(List.of(task));

        when(repository.findAllByUser(user, pageable)).thenReturn(page);
        when(mapper.toResponse(task)).thenReturn(expected);

        Page<TaskResponse> response = service.getTasks(user, pageable, null);

        assertThat(response.getContent()).containsExactly(expected);
    }

    @Test
    void getTasks_con_status_delega_en_findAllByUserAndStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = new Task();
        TaskResponse expected = sampleResponse(UUID.randomUUID(), TaskStatus.COMPLETED);
        Page<Task> page = new PageImpl<>(List.of(task));

        when(repository.findAllByUserAndStatus(user, TaskStatus.COMPLETED, pageable)).thenReturn(page);
        when(mapper.toResponse(task)).thenReturn(expected);

        Page<TaskResponse> response = service.getTasks(user, pageable, TaskStatus.COMPLETED);

        assertThat(response.getContent()).containsExactly(expected);
        verify(repository, never()).findAllByUser(any(User.class), any(Pageable.class));
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
                "x", "y", TaskStatus.PENDING, Priority.LOW, null, null);
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

    @Test
    void findTasksDueToday_delega_en_el_repository_con_la_fecha_de_hoy_y_excluye_completadas() {
        Task task = new Task();
        LocalDate today = LocalDate.now();
        when(repository.findAllByDueDateAndStatusNot(today, TaskStatus.COMPLETED)).thenReturn(List.of(task));

        List<Task> dueToday = service.findTasksDueToday();

        assertThat(dueToday).containsExactly(task);
    }
}
