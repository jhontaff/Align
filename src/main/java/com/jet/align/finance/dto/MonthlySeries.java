package com.jet.align.finance.dto;

import java.time.YearMonth;
import java.util.List;

public record MonthlySeries(
        YearMonth month,
        List<DailyAmount> days
) {
}
