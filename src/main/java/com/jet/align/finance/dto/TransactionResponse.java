package com.jet.align.finance.dto;

import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        Category category,
        String description,
        LocalDate date,
        Instant createdAt,
        Instant updatedAt
) {
}
