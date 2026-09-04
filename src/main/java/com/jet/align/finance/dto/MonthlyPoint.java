package com.jet.align.finance.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyPoint(
        YearMonth month,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance
) {
}
