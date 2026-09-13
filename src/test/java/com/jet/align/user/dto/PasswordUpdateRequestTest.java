package com.jet.align.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUpdateRequestTest {

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

    private Set<String> violatedPaths(PasswordUpdateRequest request) {
        Set<ConstraintViolation<PasswordUpdateRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void validRequest_hasNoViolations() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("OldPassw0rd", "NewPassw0rd", "NewPassw0rd");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void currentPassword_blank_failsNotBlank_butIsNotHeldToThePasswordPolicy() {
        // "abc" no cumpliría @ValidPassword; la actual solo debe estar presente.
        assertThat(violatedPaths(new PasswordUpdateRequest("abc", "NewPassw0rd", "NewPassw0rd"))).isEmpty();
        assertThat(violatedPaths(new PasswordUpdateRequest("", "NewPassw0rd", "NewPassw0rd")))
                .contains("currentPassword");
    }

    @Test
    void newPassword_weak_failsValidPassword() {
        // Prueba que @ValidPassword se aplica a un componente de record: si el @Target
        // de la constraint compuesta estuviera mal, esta anotación se ignoraría en silencio.
        assertThat(violatedPaths(new PasswordUpdateRequest("OldPassw0rd", "short", "short")))
                .contains("newPassword");
        assertThat(violatedPaths(new PasswordUpdateRequest("OldPassw0rd", "nouppercase1", "nouppercase1")))
                .contains("newPassword");
    }

    @Test
    void confirmPassword_mismatched_failsAsFieldError() {
        assertThat(violatedPaths(new PasswordUpdateRequest("OldPassw0rd", "NewPassw0rd", "Different1")))
                .contains("passwordConfirmed");
    }
}
