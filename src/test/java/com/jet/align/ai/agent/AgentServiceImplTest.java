package com.jet.align.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.agent.dto.AgentResponse;
import com.jet.align.ai.agent.dto.ChatHistoryResponse;
import com.jet.align.ai.agent.dto.ChatTurn;
import com.jet.align.ai.agent.execution.ToolExecutionService;
import com.jet.align.ai.agent.impl.AgentServiceImpl;
import com.jet.align.ai.llm.*;
import com.jet.align.ai.memory.ConversationMemory;
import com.jet.align.ai.memory.UserMemoryService;
import com.jet.align.ai.llm.ToolCall;
import com.jet.align.ai.tool.ToolRegistry;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.common.exception.AgentException;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AgentServiceImplTest {

    /**
     * Verifica el bucle completo del agente:
     *   1. el modelo pide la tool create_task,
     *   2. el agente la ejecuta y le devuelve el resultado,
     *   3. el modelo, al ver el resultado, responde con texto final.
     *
     * To do con dobles de prueba: ni OpenAI, ni red, ni base de datos. Lo que
     * se prueba es la ORQUESTACIÓN, no la tool ni el proveedor.
     */
    @Test
    void ejecuta_la_tool_pedida_y_devuelve_la_respuesta_final() {
        // Registro de las tools que el agente realmente ejecutó, para verificar.
        List<ToolCall> executed = new ArrayList<>();

        // Simula que ya existía una conversación previa con este usuario.
        SpyConversationMemory memory = new SpyConversationMemory(List.of(
                new UserMessage("Hola"),
                new AssistantMessage("Hola, ¿en qué ayudo?", List.of())));

        // Requests que el agente le mandó al LLM, para poder inspeccionarlas.
        List<LlmRequest> requestsSeen = new ArrayList<>();

        // LlmClient falso y guionizado: si en la historia ya hay un ToolMessage
        // (es decir, ya ejecutamos una tool) responde con texto; si no, pide
        // create_task. Esto reproduce las dos vueltas del bucle real.
        LlmClient scriptedClient = request -> {
            requestsSeen.add(request);

            boolean toolAlreadyRun = request.messages().stream()
                    .anyMatch(ToolMessage.class::isInstance);

            if (toolAlreadyRun) {
                return new LlmResponse(
                        new AssistantMessage("Tarea creada correctamente.", List.of()));
            }

            ToolCall call = new ToolCall(
                    "call_1",
                    "create_task",
                    Map.of("title", "Comprar leche", "priority", "MEDIUM"));
            return new LlmResponse(new AssistantMessage(null, List.of(call)));
        };

        // ToolExecutionService falso: registra la llamada y devuelve un resultado.
        ToolExecutionService execution = (toolCall, user) -> {
            executed.add(toolCall);
            return new ToolResult<>(
                    Map.of("id", "t1", "title", "Comprar leche"),
                    "Task created successfully.");
        };

        AgentServiceImpl agent = new AgentServiceImpl(
                scriptedClient,
                execution,
                new ToolRegistry(List.of()),   // sin tools registradas: los specs no importan aquí
                memory,
                mock(UserMemoryService.class),
                new ObjectMapper(),
                "UTC"
        );

        AgentResponse response = agent.chat("Créame una tarea para comprar leche", null);

        assertThat(response.reply()).isEqualTo("Tarea creada correctamente.");
        assertThat(executed).hasSize(1);
        assertThat(executed.get(0).name()).isEqualTo("create_task");
        assertThat(executed.get(0).arguments()).containsEntry("title", "Comprar leche");

        // El historial cargado desde memoria debe viajar en la primera request al LLM.
        assertThat(requestsSeen.get(0).messages()).contains(new UserMessage("Hola"));

        // Solo se persiste el turno visible: el mensaje del usuario y la
        // respuesta final, sin los pasos intermedios de tool-calling.
        assertThat(memory.appendCalled).isTrue();
        assertThat(memory.appendedMessages).containsExactly(
                new UserMessage("Créame una tarea para comprar leche"),
                new AssistantMessage("Tarea creada correctamente.", List.of()));
    }

    @Test
    void no_persiste_el_turno_si_se_agota_MAX_STEPS() {
        SpyConversationMemory memory = new SpyConversationMemory(List.of());

        // Nunca resuelve: siempre vuelve a pedir la misma tool.
        LlmClient neverEndingClient = request -> new LlmResponse(
                new AssistantMessage(null, List.of(
                        new ToolCall("call_x", "create_task", Map.of()))));

        ToolExecutionService execution = (toolCall, user) ->
                new ToolResult<>(Map.of(), "ok");

        AgentServiceImpl agent = new AgentServiceImpl(
                neverEndingClient,
                execution,
                new ToolRegistry(List.of()),
                memory,
                mock(UserMemoryService.class),
                new ObjectMapper(),
                "UTC"
        );

        assertThatThrownBy(() -> agent.chat("cualquier cosa", null))
                .isInstanceOf(AgentException.class);

        assertThat(memory.appendCalled).isFalse();
    }

    @Test
    void getHistory_mapea_los_turnos_persistidos_a_ChatTurn() {
        SpyConversationMemory memory = new SpyConversationMemory(List.of(
                new UserMessage("Hola"),
                new AssistantMessage("Hola, ¿en qué ayudo?", List.of())));

        // LLM y tools no deberían tocarse para leer historial: si getHistory
        // los llamara, el test explota en vez de fallar en silencio.
        AgentServiceImpl agent = new AgentServiceImpl(
                request -> { throw new UnsupportedOperationException("getHistory no debería llamar al LLM"); },
                (toolCall, user) -> { throw new UnsupportedOperationException("getHistory no debería ejecutar tools"); },
                new ToolRegistry(List.of()),
                memory,
                mock(UserMemoryService.class),
                new ObjectMapper(),
                "UTC"
        );

        ChatHistoryResponse response = agent.getHistory(null);

        assertThat(response.turns()).containsExactly(
                new ChatTurn("user", "Hola"),
                new ChatTurn("assistant", "Hola, ¿en qué ayudo?"));
    }

    @Test
    void getHistory_lanza_si_el_historial_persistido_trae_un_tipo_inesperado() {
        // No debería pasar nunca: chat() solo persiste UserMessage/AssistantMessage.
        // Si aparece, preferimos romper fuerte antes que ocultarlo.
        SpyConversationMemory memory = new SpyConversationMemory(List.of(
                new ToolMessage("call_1", "{}")));

        AgentServiceImpl agent = new AgentServiceImpl(
                request -> { throw new UnsupportedOperationException(); },
                (toolCall, user) -> { throw new UnsupportedOperationException(); },
                new ToolRegistry(List.of()),
                memory,
                mock(UserMemoryService.class),
                new ObjectMapper(),
                "UTC"
        );

        assertThatThrownBy(() -> agent.getHistory(null))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Espía de {@link ConversationMemory}: devuelve un historial fijo y
     * registra qué se le pidió persistir, para poder verificarlo en las
     * aserciones sin depender de una base de datos real.
     */
    private static class SpyConversationMemory implements ConversationMemory {

        private final List<Message> historyToReturn;
        private boolean appendCalled = false;
        private List<Message> appendedMessages;

        SpyConversationMemory(List<Message> historyToReturn) {
            this.historyToReturn = historyToReturn;
        }

        @Override
        public List<Message> loadHistory(User user) {
            return historyToReturn;
        }

        @Override
        public void append(User user, List<Message> newMessages) {
            appendCalled = true;
            appendedMessages = newMessages;
        }
    }
}
