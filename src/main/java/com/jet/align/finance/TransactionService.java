package com.jet.align.finance;

import com.jet.align.finance.dto.*;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface TransactionService {
    TransactionResponse createTransaction(TransactionRequest request, User user);
    TransactionResponse getTransactionById(UUID id, User user);
    Page<TransactionResponse> getTransactions(User user, Pageable pageable, TransactionFilter filter);
    TransactionResponse updateTransaction(UUID id, TransactionUpdateRequest request, User user);
    void deleteTransaction(UUID id, User user);
    FinancialSummaryResponse getSummary(User user, TransactionFilter filter);
    MonthlyChartResponse getMonthlyChart(User user);
    MonthlySummaryResponse getMonthlySummary(User user, MonthlySummaryFilter filter);
    CategoryBreakdownResponse getCategoryBreakdown(User user, LocalDate from, LocalDate to);

}
