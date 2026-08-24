package com.jet.align.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.memory.UserMemoryService;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ForgetFactToolTest {

    private final UserMemoryService userMemoryService = mock(UserMemoryService.class);
    private final ForgetFactTool tool = new ForgetFactTool(userMemoryService, new ObjectMapper());
    private final User user = new User();
    private final UUID memoryId = UUID.randomUUID();

    @Test
    void execute_convierte_memoryId_de_string_a_uuid_delega_en_forget_y_no_devuelve_payload() {
        // memoryId llega como String crudo, tal como lo arma el LLM a partir del
        // JSON schema -- nunca como un UUID ya parseado.
        ToolContext context = new ToolContext(user, Map.of("memoryId", memoryId.toString()));

        ToolResult<Void> result = tool.execute(context);

        verify(userMemoryService).forget(user, memoryId);
        assertThat(result.payload()).isNull();
    }
}
