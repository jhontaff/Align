package com.jet.align.user;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.user.dto.AvatarContent;
import com.jet.align.user.dto.EmailUpdateRequest;
import com.jet.align.user.dto.PasswordUpdateRequest;
import com.jet.align.user.dto.ProfileUpdateRequest;
import com.jet.align.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserController controller = new UserController(userService);
    private final User user = new User();

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G'};

    private UserResponse sampleResponse(String firstName, String lastName) {
        return new UserResponse(UUID.randomUUID(), "jhon@align.dev", firstName, lastName, Role.USER, Instant.now(), false);
    }

    @Test
    void me_devuelve_200_con_el_perfil_del_service() {
        UserResponse expected = sampleResponse("Jhon", "Tafur");
        when(userService.getProfile(user)).thenReturn(expected);

        ResponseEntity<ApiResponse<UserResponse>> response = controller.me(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void updateProfile_devuelve_200_con_el_perfil_actualizado_por_el_service() {
        ProfileUpdateRequest request = new ProfileUpdateRequest("Jhon Alexander", "Tafur");
        UserResponse expected = sampleResponse("Jhon Alexander", "Tafur");
        when(userService.updateProfile(user, request)).thenReturn(expected);

        ResponseEntity<ApiResponse<UserResponse>> response = controller.updateProfile(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void changePassword_delega_al_service_y_devuelve_200_sin_datos() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("OldPassw0rd", "NewPassw0rd", "NewPassw0rd");

        ResponseEntity<ApiResponse<Void>> response = controller.changePassword(request, user);

        verify(userService).changePassword(user, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void changeEmail_devuelve_200_con_el_perfil_actualizado_por_el_service() {
        EmailUpdateRequest request = new EmailUpdateRequest("nuevo@align.dev", "OldPassw0rd");
        UserResponse expected = new UserResponse(UUID.randomUUID(), "nuevo@align.dev", "Jhon", "Tafur", Role.USER, Instant.now(), false);
        when(userService.changeEmail(user, request)).thenReturn(expected);

        ResponseEntity<ApiResponse<UserResponse>> response = controller.changeEmail(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void uploadAvatar_desarma_el_multipart_en_un_AvatarContent_y_devuelve_200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", PNG_BYTES);

        ResponseEntity<ApiResponse<Void>> response = controller.uploadAvatar(file, user);

        ArgumentCaptor<AvatarContent> captor = ArgumentCaptor.forClass(AvatarContent.class);
        verify(userService).saveAvatar(eq(user), captor.capture());
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
        assertThat(captor.getValue().data()).isEqualTo(PNG_BYTES);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAvatar_devuelve_los_bytes_con_el_content_type_de_la_imagen() {
        when(userService.getAvatar(user)).thenReturn(new AvatarContent("image/webp", PNG_BYTES));

        ResponseEntity<byte[]> response = controller.getAvatar(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("image/webp"));
        assertThat(response.getBody()).isEqualTo(PNG_BYTES);
    }

    @Test
    void deleteAvatar_delega_al_service_y_devuelve_200_sin_datos() {
        ResponseEntity<ApiResponse<Void>> response = controller.deleteAvatar(user);

        verify(userService).deleteAvatar(user);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isNull();
    }
}
