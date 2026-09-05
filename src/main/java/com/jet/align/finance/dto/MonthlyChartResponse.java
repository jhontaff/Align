package com.jet.align.finance.dto;

public record MonthlyChartResponse(
        MonthlySeries currentMonth,
        MonthlySeries previousMonth
) {
}
