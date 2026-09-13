package com.jet.align.user.dto;

import com.jet.align.common.validation.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(

        // Es la contraseña que el usuario ya tiene: se exige presente, no que cumpla la
        // política actual (si la política se endureció, la vieja podría no cumplirla).
        @NotBlank(message = "La contraseña actual es obligatoria.")
        String currentPassword,

        @ValidPassword
        String newPassword,

        @NotBlank(message = "La confirmación de la contraseña es obligatoria.")
        String confirmPassword

) {

        @AssertTrue(message = "La contraseña y su confirmación no coinciden.")
        public boolean isPasswordConfirmed() {
                return newPassword != null && newPassword.equals(confirmPassword);
        }
}
