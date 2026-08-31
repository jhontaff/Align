package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.task.TaskService;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListTasksToolTest {

    // A diferencia de UpdateTaskTool, acá no hay merge parcial, así que no hace
    // falta el patrón TaskPatch + convertValue: "status" es un único filtro
    // opcional, convertido explícitamente con TaskStatus.valueOf(...). Igual que
    // en UpdateTaskTool, el punto a probar es que "status" siempre llega como
    // String crudo desde el LLM (nunca ya como TaskStatus), así que el Map de
    // argumentos usa Strings, tal como lo produciría un tool call real.

    private final TaskService taskService = mock(TaskService.class);
    private final ListTasksTool tool = new ListTasksTool(taskService, new ObjectMapper());
    private final User user = new User();

    private TaskResponse task(String title, TaskStatus status) {
        return new TaskResponse(
                UUID.randomUUID(),
                title,
                "desc",
                status,
                Priority.MEDIUM,
                LocalDate.of(2026, 8, 3),
                LocalTime.of(6, 0),
                Instant.now(),
                Instant.now());
    }

    @Test
    void sin_status_en_los_argumentos_lista_todas_las_tareas_sin_filtrar() {
        List<TaskResponse> tasks = List.of(
                task("Comprar proteína", TaskStatus.PENDING),
                task("Comprar salchichón", TaskStatus.PENDING));
        when(taskService.getTasks(eq(user), any(Pageable.class), isNull()))
                .thenReturn(new PageImpl<>(tasks));

        ToolResult<List<TaskResponse>> result = tool.execute(new ToolContext(user, Map.of()));

        // El resultado es la lista de contenido tal cual, no el Page envuelto con
        // metadata de paginación (pageable, totalElements, etc.) -- eso es
        // justamente lo que decidimos no exponerle al LLM.
        assertThat(result.payload()).containsExactlyElementsOf(tasks);
    }

    @Test
    void status_recibido_como_string_crudo_se_convierte_al_enum_correcto_para_filtrar() {
        List<TaskResponse> pending = List.of(task("Comprar proteína", TaskStatus.PENDING));
        when(taskService.getTasks(eq(user), any(Pageable.class), eq(TaskStatus.PENDING)))
                .thenReturn(new PageImpl<>(pending));

        // "status" llega como String, tal como lo arma el LLM a partir del JSON
        // schema -- nunca como un TaskStatus ya instanciado.
        ToolResult<List<TaskResponse>> result =
                tool.execute(new ToolContext(user, Map.of("status", "PENDING")));

        assertThat(result.payload()).containsExactlyElementsOf(pending);
        verify(taskService).getTasks(eq(user), any(Pageable.class), eq(TaskStatus.PENDING));
    }

    @Test
    void usa_el_mismo_tamano_y_orden_de_pagina_por_defecto_que_el_endpoint_rest() {
        when(taskService.getTasks(eq(user), any(Pageable.class), isNull()))
                .thenReturn(new PageImpl<>(List.of()));

        tool.execute(new ToolContext(user, Map.of()));

        // El LLM no controla paginación (decisión de diseño: YAGNI, ver CLAUDE.md).
        // Este test guarda ese contrato: si alguien agrega page/size al schema sin
        // querer, o cambia el default, se entera acá en vez de en producción.
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).getTasks(eq(user), captor.capture(), isNull());
        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
