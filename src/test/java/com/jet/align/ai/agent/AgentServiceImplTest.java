package com.jet.align.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.agent.execution.ToolExecutionService;
import com.jet.align.ai.agent.impl.AgentServiceImpl;
import com.jet.align.ai.llm.AssistantMessage;
import com.jet.align.ai.llm.LlmClient;
import com.jet.align.ai.llm.LlmResponse;
import com.jet.align.ai.llm.ToolMessage;
import com.jet.align.ai.model.ToolCall;
import com.jet.align.ai.tool.ToolRegistry;
import com.jet.align.ai.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentServiceImplTest {

    /**
     * Verifica el bucle completo del agente:
     *   1. el modelo pide la tool create_task,
     *   2. el agente la ejecuta y le devuelve el resultado,
     *   3. el modelo, al ver el resultado, responde con texto final.
     *
     * Todo con dobles de prueba: ni OpenAI, ni red, ni base de datos. Lo que
     * se prueba es la ORQUESTACIÓN, no la tool ni el proveedor.
     */
    @Test
    void ejecuta_la_tool_pedida_y_devuelve_la_respuesta_final() {
        // Registro de las tools que el agente realmente ejecutó, para verificar.
        List<ToolCall> executed = new ArrayList<>();

        // LlmClient falso y guionizado: si en la historia ya hay un ToolMessage
        // (es decir, ya ejecutamos una tool) responde con texto; si no, pide
        // create_task. Esto reproduce las dos vueltas del bucle real.
        LlmClient scriptedClient = request -> {
            boolean toolAlreadyRun = request.messages().stream()
                    .anyMatch(message -> message instanceof ToolMessage);

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
                new ObjectMapper());

        AgentResponse response = agent.chat("Créame una tarea para comprar leche", null);

        assertThat(response.reply()).isEqualTo("Tarea creada correctamente.");
        assertThat(executed).hasSize(1);
        assertThat(executed.get(0).name()).isEqualTo("create_task");
        assertThat(executed.get(0).arguments()).containsEntry("title", "Comprar leche");
    }
}
