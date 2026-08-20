package com.jet.align.task;

import com.jet.align.common.response.ApiResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskController controller = new TaskController(taskService);
    private final User user = new User();

    private TaskResponse sampleResponse(UUID id, TaskStatus status) {
        return new TaskResponse(
                id, "Comprar leche", "Ir al super", status, Priority.MEDIUM,
                LocalDate.of(2026, 8, 25), LocalTime.of(14, 30), Instant.now(), Instant.now());
    }

    @Test
    void createTask_devuelve_201_con_la_tarea_creada_por_el_service() {
        TaskRequest request = new TaskRequest(
                "Comprar leche", "Ir al super", Priority.MEDIUM,
                LocalDate.of(2026, 8, 25), LocalTime.of(14, 30));
        TaskResponse expected = sampleResponse(UUID.randomUUID(), TaskStatus.PENDING);
        when(taskService.createTask(request, user)).thenReturn(expected);

        ResponseEntity<ApiResponse<TaskResponse>> response = controller.createTask(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void getTaskById_devuelve_200_con_la_tarea_del_service() {
        UUID id = UUID.randomUUID();
        TaskResponse expected = sampleResponse(id, TaskStatus.PENDING);
        when(taskService.getTaskById(id, user)).thenReturn(expected);

        ResponseEntity<ApiResponse<TaskResponse>> response = controller.getTaskById(id, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void getTasks_pasa_el_status_y_el_pageable_tal_cual_al_service() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TaskResponse> expected = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), TaskStatus.PENDING)));
        when(taskService.getTasks(user, pageable, TaskStatus.PENDING)).thenReturn(expected);

        ResponseEntity<ApiResponse<Page<TaskResponse>>> response =
                controller.getTasks(user, TaskStatus.PENDING, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void updateTask_devuelve_200_con_la_tarea_actualizada() {
        UUID id = UUID.randomUUID();
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Comprar pan", "Panadería", TaskStatus.IN_PROGRESS, Priority.HIGH,
                LocalDate.of(2026, 8, 26), null);
        TaskResponse expected = sampleResponse(id, TaskStatus.IN_PROGRESS);
        when(taskService.updateTask(id, request, user)).thenReturn(expected);

        ResponseEntity<ApiResponse<TaskResponse>> response = controller.updateTask(id, request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void deleteTask_delega_en_el_service_y_devuelve_200_sin_body() {
        UUID id = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> response = controller.deleteTask(id, user);

        verify(taskService).deleteTask(id, user);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isNull();
    }
}
