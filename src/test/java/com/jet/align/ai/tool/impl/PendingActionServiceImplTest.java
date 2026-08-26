package com.jet.align.ai.tool.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.tool.PendingAction;
import com.jet.align.ai.tool.PendingActionRepository;
import com.jet.align.ai.tool.PendingActionStatus;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolRegistry;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PendingActionServiceImplTest {

    private final PendingActionRepository repository = mock(PendingActionRepository.class);
    private final ToolRegistry toolRegistry = mock(ToolRegistry.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PendingActionServiceImpl service =
            new PendingActionServiceImpl(repository, toolRegistry, objectMapper,24);
    private final User user = new User();

    private PendingAction pendingActionOf(String toolName, Map<String, Object> arguments, PendingActionStatus status) {
        PendingAction pending = new PendingAction();
        pending.setUser(user);
        pending.setToolName(toolName);
        pending.setStatus(status);
        try {
            pending.setArgumentsJson(objectMapper.writeValueAsString(arguments));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return pending;
    }

    @Test
    void create_guarda_la_pending_action_con_los_argumentos_serializados_y_estado_pending() {
        Map<String, Object> arguments = Map.of("taskId", "abc-123");
        when(repository.save(any(PendingAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PendingAction result = service.create(user, "delete_task", arguments);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getToolName()).isEqualTo("delete_task");
        assertThat(result.getStatus()).isEqualTo(PendingActionStatus.PENDING);
        assertThat(result.getArgumentsJson()).contains("abc-123");
    }

    @Test
    void confirm_ejecuta_la_tool_con_los_argumentos_originales_y_marca_la_accion_como_confirmed() {
        UUID id = UUID.randomUUID();
        Map<String, Object> originalArguments = Map.of("taskId", "abc-123");
        PendingAction pending = pendingActionOf("delete_task", originalArguments, PendingActionStatus.PENDING);
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(pending));

        Tool<Void> tool = mock(Tool.class);
        ToolResult<Void> expected = new ToolResult<>(null, "Task deleted successfully.");
        when(tool.execute(any(ToolContext.class))).thenReturn(expected);
        when(toolRegistry.get("delete_task")).thenReturn(Optional.of(tool));

        ToolResult<?> result = service.confirm(user, id);

        // Lo central de este test: la tool tiene que ejecutarse con los MISMOS
        // argumentos que se guardaron al crear la pending action, no con algo que
        // el LLM reconstruya -- ese era todo el punto de diseñar el mecanismo así.
        ArgumentCaptor<ToolContext> captor = ArgumentCaptor.forClass(ToolContext.class);
        verify(tool).execute(captor.capture());
        assertThat(captor.getValue().user()).isEqualTo(user);
        assertThat(captor.getValue().arguments()).isEqualTo(originalArguments);
        assertThat(pending.getStatus()).isEqualTo(PendingActionStatus.CONFIRMED);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void confirm_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirm_lanza_BusinessException_si_la_accion_ya_fue_resuelta() {
        UUID id = UUID.randomUUID();
        PendingAction pending = pendingActionOf("delete_task", Map.of(), PendingActionStatus.CONFIRMED);
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.confirm(user, id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reject_marca_la_accion_como_rejected_cuando_esta_pendiente() {
        UUID id = UUID.randomUUID();
        PendingAction pending = pendingActionOf("delete_task", Map.of(), PendingActionStatus.PENDING);
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(pending));

        service.reject(user, id);

        assertThat(pending.getStatus()).isEqualTo(PendingActionStatus.REJECTED);
    }

    @Test
    void reject_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reject_lanza_BusinessException_si_la_accion_ya_fue_resuelta() {
        UUID id = UUID.randomUUID();
        PendingAction pending = pendingActionOf("delete_task", Map.of(), PendingActionStatus.REJECTED);
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.reject(user, id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void expireStale_marca_como_expired_las_pending_actions_devueltas_por_el_repository() {
        PendingAction stale1 = pendingActionOf("delete_task", Map.of(), PendingActionStatus.PENDING);
        PendingAction stale2 = pendingActionOf("update_memory", Map.of(), PendingActionStatus.PENDING);
        List<PendingAction> staleActions = List.of(stale1, stale2);
        when(repository.findByStatusAndCreatedAtBefore(eq(PendingActionStatus.PENDING), any(Instant.class)))
                .thenReturn(staleActions);

        service.expireStale();

        assertThat(stale1.getStatus()).isEqualTo(PendingActionStatus.EXPIRED);
        assertThat(stale2.getStatus()).isEqualTo(PendingActionStatus.EXPIRED);
        verify(repository).saveAll(staleActions);
    }

    @Test
    void expireStale_calcula_el_cutoff_a_partir_del_ttl_configurado() {
        when(repository.findByStatusAndCreatedAtBefore(eq(PendingActionStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());

        Instant before = Instant.now();
        service.expireStale();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findByStatusAndCreatedAtBefore(eq(PendingActionStatus.PENDING), cutoffCaptor.capture());

        // El servicio se construyó con ttlHours = 24 (ver campo `service` arriba).
        assertThat(cutoffCaptor.getValue())
                .isBetween(before.minus(24, ChronoUnit.HOURS), after.minus(24, ChronoUnit.HOURS));
    }
}
