package com.jet.align.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 25)
        @Pattern(
                regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*",
                message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit."
        )
        String password,

        @NotBlank
        String confirmPassword,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName

) {

        @AssertTrue(message = "Password and confirmation must match.")
        public boolean isPasswordConfirmed() {
                return password != null && password.equals(confirmPassword);
        }
}