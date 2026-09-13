package com.jet.align.auth.dto;

import com.jet.align.common.validation.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        String email,

        @ValidPassword
        String password,

        @NotBlank(message = "La confirmación de la contraseña es obligatoria.")
        String confirmPassword,

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres.")
        String lastName

) {

        @AssertTrue(message = "La contraseña y su confirmación no coinciden.")
        public boolean isPasswordConfirmed() {
                return password != null && password.equals(confirmPassword);
        }
}