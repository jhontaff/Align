# 0026 — Daily current-vs-previous-month chart (`/monthly-chart`)

- **Status:** Accepted
- **Date:** 2026-09-04 (commit `17451be`)
- **Origin:** reconstructed from code on 2026-10-27. It was never documented when it was built.

## Decision

- `GET /api/transactions/monthly-chart` takes no parameters. It returns `MonthlyChartResponse { MonthlySeries currentMonth; MonthlySeries previousMonth }`, where `MonthlySeries { YearMonth month; List<DailyAmount> days }` and `DailyAmount { date, income, expense }`.
- `TransactionServiceImpl.getMonthlyChart` runs one `TransactionSpecifications.withFilter` query from day 1 of the previous month to the end of the current month. It groups by date in Java and splits the result into the two series, each sorted by date. This is the same aggregation shape as the rest of [0012](0012-finance-domain.md): a single query, bucketed in Java, no DB `GROUP BY`.
- The same commit changed `getMonthlySummary` to take `MonthlySummaryFilter` (`YearMonth from/to`, `type`, `category`), bound with `@ModelAttribute`.
- No AI tool, for the same reason as the other chart endpoints.

## Open points (not decided when built)

- **Days without transactions are omitted, not zero-filled.** `getMonthlySummary` zero-fills its months specifically so the chart axis has no holes. If this chart draws a continuous daily line, the frontend has to fill those gaps itself. Confirm which side owns that.
- **Path naming.** Every other aggregate lives under `/summary/*` (`/summary`, `/summary/monthly`, `/summary/by-category`); this one is `/monthly-chart`. Renaming it would break the frontend, so it stays as is unless there's a reason to align it.
- No `from`/`to`: the range is always the current and previous month in `align.timezone`.
