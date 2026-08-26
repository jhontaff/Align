package com.jet.align.ai.agent.execution;

import com.jet.align.ai.llm.ToolCall;
import com.jet.align.ai.tool.PendingAction;
import com.jet.align.ai.tool.PendingActionService;
import com.jet.align.ai.tool.RiskLevel;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolRegistry;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ToolExecutionServiceImplTest {

    private final PendingActionService pendingActionService = mock(PendingActionService.class);
    private final User user = new User();

    @Test
    void execute_ejecuta_la_tool_directamente_cuando_el_riesgo_es_SAFE() {
        Tool<String> tool = mock(Tool.class);
        when(tool.name()).thenReturn("list_tasks");
        when(tool.risk()).thenReturn(RiskLevel.SAFE);
        ToolResult<String> expected = new ToolResult<>("ok", "Tasks retrieved successfully.");
        when(tool.execute(any(ToolContext.class))).thenReturn(expected);

        ToolExecutionServiceImpl service =
                new ToolExecutionServiceImpl(new ToolRegistry(List.of(tool)), pendingActionService);

        ToolResult<?> result = service.execute(new ToolCall("call_1", "list_tasks", Map.of()), user);

        assertThat(result).isEqualTo(expected);
        verifyNoInteractions(pendingActionService);
    }

    // El corazón de la Fase 3: una tool DESTRUCTIVE nunca se ejecuta en el primer
    // intento -- solo se registra como pendiente, y lo que recibe el LLM no es el
    // resultado real de la tool, sino la señal de que hace falta confirmación.
    @Test
    void execute_crea_una_pending_action_y_no_ejecuta_la_tool_cuando_el_riesgo_es_DESTRUCTIVE() {
        Tool<Void> tool = mock(Tool.class);
        when(tool.name()).thenReturn("delete_task");
        when(tool.risk()).thenReturn(RiskLevel.DESTRUCTIVE);

        UUID pendingId = UUID.randomUUID();
        PendingAction pending = mock(PendingAction.class);
        when(pending.getId()).thenReturn(pendingId);

        Map<String, Object> arguments = Map.of("taskId", "abc-123");
        when(pendingActionService.create(user, "delete_task", arguments)).thenReturn(pending);

        ToolExecutionServiceImpl service =
                new ToolExecutionServiceImpl(new ToolRegistry(List.of(tool)), pendingActionService);

        ToolResult<?> result = service.execute(new ToolCall("call_1", "delete_task", arguments), user);

        verify(tool, never()).execute(any());
        assertThat(result.payload()).isEqualTo(Map.of("pendingActionId", pendingId.toString()));
    }

    @Test
    void execute_lanza_ResourceNotFoundException_si_la_tool_no_existe() {
        ToolExecutionServiceImpl service =
                new ToolExecutionServiceImpl(new ToolRegistry(List.of()), pendingActionService);

        assertThatThrownBy(() ->
                service.execute(new ToolCall("call_1", "unknown_tool", Map.of()), user))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
