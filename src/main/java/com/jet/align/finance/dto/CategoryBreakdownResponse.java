package com.jet.align.finance.dto;

import java.util.List;

public record CategoryBreakdownResponse(
        List<CategoryAmount> expenses,
        List<CategoryAmount> incomes
) {
}
