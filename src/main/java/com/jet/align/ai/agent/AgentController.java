package com.jet.align.ai.agent;

import com.jet.align.ai.agent.dto.AgentResponse;
import com.jet.align.ai.agent.dto.ChatHistoryResponse;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Punto de entrada del producto: el usuario conversa con el agente.
 *
 * <p>Delgado a propósito: solo extrae el usuario autenticado y delega en el
 * {@link AgentService}. Toda la lógica del bucle vive en el servicio.
 */
@RestController
@RequestMapping("api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    public AgentResponse chat(
            @RequestBody ChatMessage request,
            @AuthenticationPrincipal User user
    ) {
        return agentService.chat(request.message(), user);
    }

    @GetMapping("/history")
    public ChatHistoryResponse getHistory(@AuthenticationPrincipal User user) {
        return agentService.getHistory(user);
    }


    public record ChatMessage(String message) {
    }
}
