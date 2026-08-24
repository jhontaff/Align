package com.jet.align.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.ai.memory.UserMemoryService;
import com.jet.align.ai.memory.dto.MemoryResponse;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListMemoriesToolTest {

    private final UserMemoryService userMemoryService = mock(UserMemoryService.class);
    private final ListMemoriesTool tool = new ListMemoriesTool(userMemoryService, new ObjectMapper());
    private final User user = new User();

    @Test
    void execute_delega_en_list_y_devuelve_la_lista_tal_cual() {
        List<MemoryResponse> memories = List.of(
                new MemoryResponse(UUID.randomUUID(), "Vive en Lima", Instant.now()),
                new MemoryResponse(UUID.randomUUID(), "Trabaja remoto", Instant.now()));
        when(userMemoryService.list(user)).thenReturn(memories);

        // list_memories no toma argumentos -- el schema no declara properties, así
        // que el LLM manda un Map vacío.
        ToolResult<List<MemoryResponse>> result = tool.execute(new ToolContext(user, Map.of()));

        assertThat(result.payload()).containsExactlyElementsOf(memories);
    }
}
