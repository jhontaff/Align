package com.jet.align.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.memory.UserMemoryService;
import com.jet.align.ai.memory.dto.MemoryResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RememberFactToolTest {

    private final UserMemoryService userMemoryService = mock(UserMemoryService.class);
    private final RememberFactTool tool = new RememberFactTool(userMemoryService, new ObjectMapper());
    private final User user = new User();

    @Test
    void execute_pasa_el_content_crudo_del_llm_tal_cual_a_remember() {
        MemoryResponse expected = new MemoryResponse(
                UUID.randomUUID(), "Prefiere que le hable de usted", Instant.now());
        when(userMemoryService.remember(user, "Prefiere que le hable de usted")).thenReturn(expected);

        // content llega como String crudo, tal como lo arma el LLM a partir del
        // JSON schema -- no hay DTO de por medio que convertir.
        ToolContext context = new ToolContext(user, Map.of("content", "Prefiere que le hable de usted"));

        ToolResult<MemoryResponse> result = tool.execute(context);

        verify(userMemoryService).remember(user, "Prefiere que le hable de usted");
        assertThat(result.payload()).isEqualTo(expected);
    }
}
