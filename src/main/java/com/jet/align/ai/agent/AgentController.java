package com.jet.align.ai.agent;

import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public record ChatMessage(String message) {
    }
}
