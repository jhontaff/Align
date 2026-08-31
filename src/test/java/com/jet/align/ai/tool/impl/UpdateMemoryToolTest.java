package com.jet.align.ai.tool.impl;

import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

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

class UpdateMemoryToolTest {

    private final UserMemoryService userMemoryService = mock(UserMemoryService.class);
    private final UpdateMemoryTool tool = new UpdateMemoryTool(userMemoryService, new ObjectMapper());
    private final User user = new User();
    private final UUID memoryId = UUID.randomUUID();

    @Test
    void execute_convierte_memoryId_de_string_a_uuid_y_delega_en_update() {
        MemoryResponse expected = new MemoryResponse(memoryId, "Vive en Cusco", Instant.now());
        when(userMemoryService.update(user, memoryId, "Vive en Cusco")).thenReturn(expected);

        // memoryId y content llegan como Strings crudos, tal como los arma el LLM
        // a partir del JSON schema -- memoryId nunca como UUID ya parseado.
        ToolContext context = new ToolContext(user, Map.of(
                "memoryId", memoryId.toString(),
                "content", "Vive en Cusco"));

        ToolResult<MemoryResponse> result = tool.execute(context);

        verify(userMemoryService).update(user, memoryId, "Vive en Cusco");
        assertThat(result.payload()).isEqualTo(expected);
    }
}
