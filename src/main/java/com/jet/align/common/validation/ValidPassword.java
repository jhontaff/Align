package com.jet.align.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Regla única de contraseña, compartida por el registro y el cambio de contraseña.
 * Constraint compuesta: no tiene validator propio, delega en las tres anotaciones
 * que la componen, y cada una reporta su propio mensaje.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "La contraseña es obligatoria.")
@Size(min = 8, max = 25, message = "La contraseña debe tener entre 8 y 25 caracteres.")
@Pattern(
        regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*",
        message = "La contraseña debe incluir al menos una minúscula, una mayúscula y un número."
)
public @interface ValidPassword {

    String message() default "La contraseña no es válida.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
