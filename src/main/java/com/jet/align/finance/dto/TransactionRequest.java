package com.jet.align.finance.dto;

import com.jet.align.finance.enums.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(

        @NotNull(message = "Amount is required.")
        @Positive(message = "Amount must be greater than zero.")
        BigDecimal amount,

        @NotNull(message = "Category is required.")
        Category category,

        @Size(max = 255, message = "Description cannot exceed 255 characters.")
        String description,

        LocalDate  date
) {
}
