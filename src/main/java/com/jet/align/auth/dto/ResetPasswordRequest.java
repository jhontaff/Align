package com.jet.align.auth.dto;

import com.jet.align.common.validation.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(

        @NotBlank(message = "El token es obligatorio.")
        String token,

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
