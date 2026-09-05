package com.jet.align.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyAmount(
        LocalDate date,
        BigDecimal income,
        BigDecimal expense
) {
}
