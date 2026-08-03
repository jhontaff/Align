package com.jet.align.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.task.TaskService;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.dto.TaskUpdateRequest;
import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateTaskToolTest {

    // Antes del fix, execute() casteaba directo:
    //   (Priority) context.arguments().getOrDefault("priority", current.priority().name())
    // Los argumentos de una tool siempre llegan como Map<String, Object> deserializado
    // de JSON (así los produce el LLM), así que "priority" es un String ("HIGH"),
    // nunca un Priority ya parseado. Ese cast tiraba ClassCastException siempre que
    // se llamaba a update_task, incluso sin pisar priority: el propio fallback
    // (current.priority().name()) es un String casteado a Priority.
    //
    // Por eso en los dos tests de abajo el Map de argumentos usa Strings crudos para
    // los campos que antes reventaban (status, priority, dueDate, dueTime) — si alguien
    // reintroduce un cast en vez de pasar por el TaskPatch + ObjectMapper#convertValue,
    // estos tests fallan (o directamente explotan con ClassCastException).

    private final TaskService taskService = mock(TaskService.class);
    // Spring Boot registra jackson-datatype-jsr310 automáticamente en el ObjectMapper
    // real de la app (por eso CreateTaskTool ya parsea dueDate en producción). Un
    // ObjectMapper pelado no lo tiene, así que lo registramos a mano para que el
    // ObjectMapper del test se comporte igual que el bean real de Spring.
    private final UpdateTaskTool tool = new UpdateTaskTool(
            taskService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();
    private final UUID taskId = UUID.randomUUID();

    private TaskResponse currentTask() {
        return new TaskResponse(
                taskId,
                "Comprar leche",
                "En el supermercado de la esquina",
                TaskStatus.PENDING,
                Priority.LOW,
                LocalDate.of(2026, 8, 1),
                LocalTime.of(9, 0),
                Instant.now(),
                Instant.now());
    }

    @Test
    void patch_parcial_conserva_los_campos_no_enviados_del_estado_actual() {
        TaskResponse current = currentTask();
        when(taskService.getTaskById(taskId, user)).thenReturn(current);
        when(taskService.updateTask(eq(taskId), any(TaskUpdateRequest.class), eq(user)))
                .thenReturn(current);

        // Solo se manda taskId + status, tal como lo armaría el LLM si el usuario
        // dice "marcá esa tarea como en progreso" sin mencionar nada más.
        ToolContext context = new ToolContext(user, Map.of(
                "taskId", taskId.toString(),
                "status", "IN_PROGRESS"));

        tool.execute(context);

        ArgumentCaptor<TaskUpdateRequest> captor = ArgumentCaptor.forClass(TaskUpdateRequest.class);
        verify(taskService).updateTask(eq(taskId), captor.capture(), eq(user));
        TaskUpdateRequest merged = captor.getValue();

        // El único campo enviado se actualiza...
        assertThat(merged.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        // ...todo lo demás viaja tal cual estaba en current, no null ni un valor default.
        assertThat(merged.title()).isEqualTo(current.title());
        assertThat(merged.description()).isEqualTo(current.description());
        assertThat(merged.priority()).isEqualTo(current.priority());
        assertThat(merged.dueDate()).isEqualTo(current.dueDate());
        assertThat(merged.dueTime()).isEqualTo(current.dueTime());
    }

    @Test
    void patch_completo_pisa_todos_los_campos_parseando_enums_y_fechas_desde_strings() {
        TaskResponse current = currentTask();
        when(taskService.getTaskById(taskId, user)).thenReturn(current);
        when(taskService.updateTask(eq(taskId), any(TaskUpdateRequest.class), eq(user)))
                .thenReturn(current);

        // Todos los campos presentes, como Strings crudos (igual que llegarían del LLM).
        ToolContext context = new ToolContext(user, Map.of(
                "taskId", taskId.toString(),
                "title", "Comprar leche deslactosada",
                "description", "Marca sin lactosa",
                "status", "COMPLETED",
                "priority", "HIGH",
                "dueDate", "2026-08-10",
                "dueTime", "18:30"));

        tool.execute(context);

        ArgumentCaptor<TaskUpdateRequest> captor = ArgumentCaptor.forClass(TaskUpdateRequest.class);
        verify(taskService).updateTask(eq(taskId), captor.capture(), eq(user));
        TaskUpdateRequest merged = captor.getValue();

        assertThat(merged.title()).isEqualTo("Comprar leche deslactosada");
        assertThat(merged.description()).isEqualTo("Marca sin lactosa");
        assertThat(merged.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(merged.priority()).isEqualTo(Priority.HIGH);
        assertThat(merged.dueDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(merged.dueTime()).isEqualTo(LocalTime.of(18, 30));
    }
}
