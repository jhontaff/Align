package com.jet.align.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ResetPasswordRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    private Set<String> violatedPaths(ResetPasswordRequest request) {
        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(new ResetPasswordRequest("tok", "NewPassw0rd", "NewPassw0rd"))).isEmpty();
    }

    @Test
    void token_blank_failsNotBlank() {
        assertThat(violatedPaths(new ResetPasswordRequest("", "NewPassw0rd", "NewPassw0rd"))).contains("token");
    }

    @Test
    void newPassword_weak_failsValidPassword() {
        // Misma regla que registro y cambio de contraseña, no una copia local.
        assertThat(violatedPaths(new ResetPasswordRequest("tok", "short", "short"))).contains("newPassword");
    }

    @Test
    void confirmPassword_mismatched_failsAsFieldError() {
        assertThat(violatedPaths(new ResetPasswordRequest("tok", "NewPassw0rd", "Different1")))
                .contains("passwordConfirmed");
    }
}
