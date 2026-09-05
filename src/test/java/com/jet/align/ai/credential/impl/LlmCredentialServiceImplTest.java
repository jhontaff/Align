package com.jet.align.ai.credential.impl;

import com.jet.align.ai.credential.LlmCredential;
import com.jet.align.ai.credential.LlmCredentialRepository;
import com.jet.align.ai.credential.dto.LlmCredentialStatusResponse;
import com.jet.align.ai.llm.LlmApiKey;
import com.jet.align.ai.llm.LlmCredentialValidator;
import com.jet.align.common.exception.LlmCredentialInvalidException;
import com.jet.align.common.exception.LlmCredentialMissingException;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LlmCredentialServiceImplTest {

    private final LlmCredentialRepository repository = mock(LlmCredentialRepository.class);
    private final LlmCredentialValidator validator = mock(LlmCredentialValidator.class);
    private final ApiKeyCipher cipher = mock(ApiKeyCipher.class);

    private final LlmCredentialServiceImpl service =
            new LlmCredentialServiceImpl(repository, validator, cipher);

    private final User user = new User();

    /**
     * El punto central del wizard: si la key no sirve, el usuario se entera en
     * el mismo paso y no queda NADA guardado.
     */
    @Test
    void save_no_persiste_si_el_proveedor_rechaza_la_key() {
        doThrow(new LlmCredentialInvalidException("key inválida", null))
                .when(validator).validate(any());

        assertThatThrownBy(() -> service.save(user, "key-que-no-sirve"))
                .isInstanceOf(LlmCredentialInvalidException.class);

        verifyNoInteractions(repository);
        verifyNoInteractions(cipher);
    }

    @Test
    void save_cifra_la_key_y_guarda_solo_los_ultimos_cuatro_caracteres() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());
        when(cipher.encrypt("AIzaSy0000ABCD")).thenReturn("cifrado");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LlmCredentialStatusResponse status = service.save(user, "AIzaSy0000ABCD");

        ArgumentCaptor<LlmCredential> saved = ArgumentCaptor.forClass(LlmCredential.class);
        verify(repository).save(saved.capture());

        assertThat(saved.getValue().getEncryptedKey()).isEqualTo("cifrado");
        assertThat(saved.getValue().getLastFour()).isEqualTo("ABCD");
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(status.configured()).isTrue();
        assertThat(status.lastFour()).isEqualTo("ABCD");
    }

    /**
     * Una sola credencial por usuario: guardar de nuevo actualiza la fila que
     * ya existe en vez de intentar insertar una segunda (que además chocaría
     * contra uk_llm_credentials_user).
     */
    @Test
    void save_reemplaza_la_credencial_existente_en_vez_de_crear_otra() {
        LlmCredential existing = new LlmCredential();
        existing.setUser(user);
        existing.setEncryptedKey("vieja");
        existing.setLastFour("0000");

        when(repository.findByUser(user)).thenReturn(Optional.of(existing));
        when(cipher.encrypt(anyString())).thenReturn("nueva-cifrada");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(user, "AIzaSyNuevaWXYZ");

        ArgumentCaptor<LlmCredential> saved = ArgumentCaptor.forClass(LlmCredential.class);
        verify(repository).save(saved.capture());

        assertThat(saved.getValue()).isSameAs(existing);
        assertThat(saved.getValue().getEncryptedKey()).isEqualTo("nueva-cifrada");
        assertThat(saved.getValue().getLastFour()).isEqualTo("WXYZ");
    }

    /**
     * Pegar una key desde el navegador arrastra espacios y saltos de línea con
     * muchísima frecuencia; si no se recortan, el proveedor la rechaza y el
     * usuario no entiende por qué.
     */
    @Test
    void save_recorta_espacios_antes_de_validar_y_cifrar() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());
        when(cipher.encrypt(anyString())).thenReturn("cifrado");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(user, "  AIzaSyConEspacios \n");

        verify(validator).validate(new LlmApiKey("AIzaSyConEspacios"));
        verify(cipher).encrypt("AIzaSyConEspacios");
    }

    @Test
    void getStatus_informa_no_configurado_en_vez_de_fallar() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        LlmCredentialStatusResponse status = service.getStatus(user);

        assertThat(status.configured()).isFalse();
        assertThat(status.lastFour()).isNull();
    }

    @Test
    void resolve_devuelve_la_key_descifrada() {
        LlmCredential credential = new LlmCredential();
        credential.setEncryptedKey("cifrado");

        when(repository.findByUser(user)).thenReturn(Optional.of(credential));
        when(cipher.decrypt("cifrado")).thenReturn(Optional.of("AIzaSyDescifrada"));

        assertThat(service.resolve(user)).isEqualTo(new LlmApiKey("AIzaSyDescifrada"));
    }

    @Test
    void resolve_lanza_missing_si_el_usuario_no_configuro_ninguna_key() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(user))
                .isInstanceOf(LlmCredentialMissingException.class);
    }

    /**
     * Rotaron align.crypto.secret: lo guardado ya no se puede leer. Se trata
     * igual que "no hay key", que es lo que manda al usuario al wizard.
     */
    @Test
    void resolve_lanza_missing_si_la_key_guardada_no_se_puede_descifrar() {
        LlmCredential credential = new LlmCredential();
        credential.setEncryptedKey("ilegible");

        when(repository.findByUser(user)).thenReturn(Optional.of(credential));
        when(cipher.decrypt("ilegible")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(user))
                .isInstanceOf(LlmCredentialMissingException.class);
    }

    @Test
    void delete_es_idempotente_si_no_habia_credencial() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        service.delete(user);

        verify(repository, never()).delete(any());
    }
}
