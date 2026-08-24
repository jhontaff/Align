package com.jet.align.ai.memory.impl;

import com.jet.align.ai.memory.UserMemory;
import com.jet.align.ai.memory.UserMemoryRepository;
import com.jet.align.ai.memory.dto.MemoryResponse;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserMemoryServiceImplTest {

    private final UserMemoryRepository repository = mock(UserMemoryRepository.class);
    private final UserMemoryServiceImpl service = new UserMemoryServiceImpl(repository);
    private final User user = new User();

    @Test
    void remember_asigna_el_usuario_autenticado_y_devuelve_la_respuesta_mapeada() {
        when(repository.save(any(UserMemory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemoryResponse response = service.remember(user, "Prefiere que le hable de usted");

        verify(repository).save(any(UserMemory.class));
        assertThat(response.content()).isEqualTo("Prefiere que le hable de usted");
    }

    @Test
    void list_devuelve_las_memorias_del_usuario_ya_mapeadas_en_el_orden_del_repositorio() {
        UserMemory older = memoryOf("Vive en Lima");
        UserMemory newer = memoryOf("Trabaja remoto");
        when(repository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(newer, older));

        List<MemoryResponse> responses = service.list(user);

        assertThat(responses).extracting(MemoryResponse::content)
                .containsExactly("Trabaja remoto", "Vive en Lima");
    }

    @Test
    void update_actualiza_el_contenido_cuando_la_memoria_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        UserMemory memory = memoryOf("Vive en Lima");
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(memory));

        MemoryResponse response = service.update(user, id, "Vive en Cusco");

        assertThat(memory.getContent()).isEqualTo("Vive en Cusco");
        assertThat(response.content()).isEqualTo("Vive en Cusco");
    }

    @Test
    void update_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(user, id, "Vive en Cusco"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void forget_elimina_la_memoria_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        UserMemory memory = memoryOf("Vive en Lima");
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(memory));

        service.forget(user, id);

        verify(repository).delete(memory);
    }

    @Test
    void forget_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.forget(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any(UserMemory.class));
    }

    private UserMemory memoryOf(String content) {
        UserMemory memory = new UserMemory();
        memory.setUser(user);
        memory.setContent(content);
        return memory;
    }
}
