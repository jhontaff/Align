package com.jet.align.finance.dto;

import com.jet.align.finance.enums.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpdateRequest(

        @NotNull(message = "El monto es obligatorio.")
        @Positive(message = "El monto debe ser mayor que cero.")
        BigDecimal amount,
        @NotNull(message = "La categoría es obligatoria.")
        Category category,
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String description,
        @NotNull(message = "La fecha es obligatoria.")
        LocalDate date
) {
}
