package com.jet.align.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailUpdateRequest(

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        String newEmail,

        @NotBlank(message = "La contraseña actual es obligatoria.")
        String currentPassword

) {}
