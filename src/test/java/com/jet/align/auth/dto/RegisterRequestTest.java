package com.jet.align.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

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

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "user@example.com",
                "Passw0rd",
                "Passw0rd",
                "Jane",
                "Doe"
        );
    }

    @Test
    void validRequest_hasNoViolations() {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void password_tooShort_failsSizeConstraint() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "Pw0aaaa", "Pw0aaaa", "Jane", "Doe"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    void password_tooLong_failsSizeConstraint() {
        String tooLong = "Aa0" + "a".repeat(23); // 26 chars total
        RegisterRequest request = new RegisterRequest(
                "user@example.com", tooLong, tooLong, "Jane", "Doe"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    void password_missingUppercase_failsPatternConstraint() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "passw0rd", "passw0rd", "Jane", "Doe"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    void password_missingLowercase_failsPatternConstraint() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "PASSW0RD", "PASSW0RD", "Jane", "Doe"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    void password_missingDigit_failsPatternConstraint() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "Password", "Password", "Jane", "Doe"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    void confirmPassword_mismatched_failsAsFieldError() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "Passw0rd", "Different1", "Jane", "Doe"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("passwordConfirmed");
    }

    @Test
    void confirmPassword_blank_failsNotBlankConstraint() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "Passw0rd", "", "Jane", "Doe"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("confirmPassword");
    }
}
