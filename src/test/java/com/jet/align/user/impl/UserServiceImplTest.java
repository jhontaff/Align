package com.jet.align.user.impl;

import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.Role;
import com.jet.align.user.User;
import com.jet.align.user.UserAvatar;
import com.jet.align.user.UserAvatarRepository;
import com.jet.align.user.UserMapper;
import com.jet.align.user.UserRepository;
import com.jet.align.user.dto.AvatarContent;
import com.jet.align.user.dto.EmailUpdateRequest;
import com.jet.align.user.dto.PasswordUpdateRequest;
import com.jet.align.user.dto.ProfileUpdateRequest;
import com.jet.align.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final UserAvatarRepository avatarRepository = mock(UserAvatarRepository.class);
    private final UserMapper mapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserServiceImpl service = new UserServiceImpl(repository, avatarRepository, mapper, passwordEncoder);

    private final UUID userId = UUID.randomUUID();
    private final User principal = userWithId(userId);

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G'};

    private static User userWithId(UUID id) {
        User user = User.builder()
                .email("jhon@align.dev")
                .password("$hashed-old")
                .firstName("Jhon")
                .lastName("Tafur")
                .role(Role.USER)
                .build();
        // BaseEntity.id no tiene setter (lo asigna JPA); para un test sin DB se fija por reflexión.
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserResponse sampleResponse(String firstName, String lastName, boolean hasAvatar) {
        return new UserResponse(userId, "jhon@align.dev", firstName, lastName, Role.USER, Instant.now(), hasAvatar);
    }

    // --- perfil -------------------------------------------------------------

    @Test
    void getProfile_mapea_el_principal_sin_recargarlo_e_informa_si_tiene_avatar() {
        UserResponse expected = sampleResponse("Jhon", "Tafur", true);
        when(avatarRepository.existsByUser(principal)).thenReturn(true);
        when(mapper.toResponse(principal, true)).thenReturn(expected);

        UserResponse response = service.getProfile(principal);

        assertThat(response).isEqualTo(expected);
        verify(repository, never()).findById(any());
    }

    @Test
    void updateProfile_recarga_el_usuario_lo_actualiza_via_mapper_y_lo_persiste() {
        User managed = userWithId(userId);
        ProfileUpdateRequest request = new ProfileUpdateRequest("Jhon Alexander", "Tafur");
        UserResponse expected = sampleResponse("Jhon Alexander", "Tafur", false);
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(repository.save(managed)).thenReturn(managed);
        when(mapper.toResponse(managed, false)).thenReturn(expected);

        UserResponse response = service.updateProfile(principal, request);

        assertThat(response).isEqualTo(expected);
        verify(mapper).updateEntity(request, managed);
        verify(repository).save(managed);
    }

    @Test
    void updateProfile_escribe_sobre_la_instancia_recargada_y_no_sobre_el_principal() {
        User managed = userWithId(userId);
        ProfileUpdateRequest request = new ProfileUpdateRequest("Jhon Alexander", "Tafur");
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(repository.save(managed)).thenReturn(managed);

        service.updateProfile(principal, request);

        verify(mapper, never()).updateEntity(request, principal);
        verify(repository, never()).save(principal);
    }

    @Test
    void updateProfile_lanza_ResourceNotFoundException_si_el_usuario_ya_no_existe() {
        ProfileUpdateRequest request = new ProfileUpdateRequest("Jhon", "Tafur");
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(principal, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    // --- contraseña ---------------------------------------------------------

    @Test
    void changePassword_verifica_la_actual_contra_el_hash_guardado_y_persiste_la_nueva_cifrada() {
        User managed = userWithId(userId);
        PasswordUpdateRequest request = new PasswordUpdateRequest("OldPassw0rd", "NewPassw0rd", "NewPassw0rd");
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(passwordEncoder.matches("OldPassw0rd", "$hashed-old")).thenReturn(true);
        when(passwordEncoder.encode("NewPassw0rd")).thenReturn("$hashed-new");

        service.changePassword(principal, request);

        assertThat(managed.getPassword()).isEqualTo("$hashed-new");
        verify(repository).save(managed);
    }

    @Test
    void changePassword_lanza_BusinessException_y_no_persiste_si_la_actual_es_incorrecta() {
        User managed = userWithId(userId);
        PasswordUpdateRequest request = new PasswordUpdateRequest("wrong", "NewPassw0rd", "NewPassw0rd");
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(passwordEncoder.matches("wrong", "$hashed-old")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La contraseña actual es incorrecta.");
        assertThat(managed.getPassword()).isEqualTo("$hashed-old");
        verify(passwordEncoder, never()).encode(any());
        verify(repository, never()).save(any());
    }

    @Test
    void changePassword_lanza_ResourceNotFoundException_si_el_usuario_ya_no_existe() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("OldPassw0rd", "NewPassw0rd", "NewPassw0rd");
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    // --- correo -------------------------------------------------------------

    @Test
    void changeEmail_verifica_la_contrasena_comprueba_disponibilidad_y_persiste_el_nuevo_correo() {
        User managed = userWithId(userId);
        EmailUpdateRequest request = new EmailUpdateRequest("nuevo@align.dev", "OldPassw0rd");
        UserResponse expected = new UserResponse(userId, "nuevo@align.dev", "Jhon", "Tafur", Role.USER, Instant.now(), false);
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(passwordEncoder.matches("OldPassw0rd", "$hashed-old")).thenReturn(true);
        when(repository.existsByEmail("nuevo@align.dev")).thenReturn(false);
        when(repository.save(managed)).thenReturn(managed);
        when(mapper.toResponse(managed, false)).thenReturn(expected);

        UserResponse response = service.changeEmail(principal, request);

        assertThat(response).isEqualTo(expected);
        assertThat(managed.getEmail()).isEqualTo("nuevo@align.dev");
        verify(repository).save(managed);
    }

    @Test
    void changeEmail_lanza_BusinessException_si_la_contrasena_es_incorrecta_sin_consultar_el_correo() {
        User managed = userWithId(userId);
        EmailUpdateRequest request = new EmailUpdateRequest("nuevo@align.dev", "wrong");
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(passwordEncoder.matches("wrong", "$hashed-old")).thenReturn(false);

        assertThatThrownBy(() -> service.changeEmail(principal, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La contraseña actual es incorrecta.");
        verify(repository, never()).existsByEmail(any());
        verify(repository, never()).save(any());
    }

    @Test
    void changeEmail_lanza_BusinessException_si_el_correo_ya_esta_registrado() {
        User managed = userWithId(userId);
        EmailUpdateRequest request = new EmailUpdateRequest("ocupado@align.dev", "OldPassw0rd");
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(passwordEncoder.matches("OldPassw0rd", "$hashed-old")).thenReturn(true);
        when(repository.existsByEmail("ocupado@align.dev")).thenReturn(true);

        assertThatThrownBy(() -> service.changeEmail(principal, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El correo electrónico ya está registrado.");
        assertThat(managed.getEmail()).isEqualTo("jhon@align.dev");
        verify(repository, never()).save(any());
    }

    @Test
    void changeEmail_con_el_mismo_correo_es_un_noop_que_devuelve_el_perfil_sin_persistir() {
        User managed = userWithId(userId);
        EmailUpdateRequest request = new EmailUpdateRequest("jhon@align.dev", "OldPassw0rd");
        UserResponse expected = sampleResponse("Jhon", "Tafur", false);
        when(repository.findById(userId)).thenReturn(Optional.of(managed));
        when(passwordEncoder.matches("OldPassw0rd", "$hashed-old")).thenReturn(true);
        when(mapper.toResponse(eq(managed), anyBoolean())).thenReturn(expected);

        UserResponse response = service.changeEmail(principal, request);

        assertThat(response).isEqualTo(expected);
        verify(repository, never()).existsByEmail(any());
        verify(repository, never()).save(any());
    }

    // --- avatar -------------------------------------------------------------

    @Test
    void saveAvatar_crea_la_fila_cuando_el_usuario_no_tenia_foto() {
        when(avatarRepository.findByUser(principal)).thenReturn(Optional.empty());

        service.saveAvatar(principal, new AvatarContent("image/png", PNG_BYTES));

        verify(avatarRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getUser() == principal
                        && saved.getContentType().equals("image/png")
                        && saved.getData() == PNG_BYTES));
    }

    @Test
    void saveAvatar_reemplaza_la_foto_existente_en_vez_de_insertar_otra() {
        UserAvatar existing = new UserAvatar();
        existing.setUser(principal);
        existing.setContentType("image/jpeg");
        existing.setData(new byte[]{1, 2, 3});
        when(avatarRepository.findByUser(principal)).thenReturn(Optional.of(existing));

        service.saveAvatar(principal, new AvatarContent("image/png", PNG_BYTES));

        assertThat(existing.getContentType()).isEqualTo("image/png");
        assertThat(existing.getData()).isSameAs(PNG_BYTES);
        verify(avatarRepository).save(existing);
    }

    @Test
    void saveAvatar_rechaza_un_archivo_vacio() {
        assertThatThrownBy(() -> service.saveAvatar(principal, new AvatarContent("image/png", new byte[0])))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La foto de perfil está vacía.");
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void saveAvatar_rechaza_un_tipo_que_no_es_imagen_permitida() {
        assertThatThrownBy(() -> service.saveAvatar(principal, new AvatarContent("image/gif", PNG_BYTES)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La foto de perfil debe ser JPEG, PNG o WebP.");
        assertThatThrownBy(() -> service.saveAvatar(principal, new AvatarContent(null, PNG_BYTES)))
                .isInstanceOf(BusinessException.class);
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void saveAvatar_rechaza_un_archivo_por_encima_del_limite() {
        byte[] tooBig = new byte[UserServiceImpl.MAX_AVATAR_BYTES + 1];

        assertThatThrownBy(() -> service.saveAvatar(principal, new AvatarContent("image/png", tooBig)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La foto de perfil no puede superar los 512 KB.");
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void getAvatar_devuelve_los_bytes_y_el_tipo_guardados() {
        UserAvatar stored = new UserAvatar();
        stored.setContentType("image/webp");
        stored.setData(PNG_BYTES);
        when(avatarRepository.findByUser(principal)).thenReturn(Optional.of(stored));

        AvatarContent avatar = service.getAvatar(principal);

        assertThat(avatar.contentType()).isEqualTo("image/webp");
        assertThat(avatar.data()).isSameAs(PNG_BYTES);
    }

    @Test
    void getAvatar_lanza_ResourceNotFoundException_si_el_usuario_no_tiene_foto() {
        when(avatarRepository.findByUser(principal)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAvatar(principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteAvatar_delega_en_el_repositorio() {
        service.deleteAvatar(principal);

        verify(avatarRepository).deleteByUser(principal);
    }
}
