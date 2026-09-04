package com.jet.align.finance.dto;

import java.util.List;

public record MonthlySummaryResponse(
        List<MonthlyPoint> months
) {
}
