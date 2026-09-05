package com.jet.align.finance.dto;

import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;

import java.time.YearMonth;

public record MonthlySummaryFilter(
        YearMonth from,
        YearMonth to,
        TransactionType type,
        Category category
) {
}
