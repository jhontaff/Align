package com.jet.align.finance.impl;

import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.finance.*;
import com.jet.align.finance.dto.*;
import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;
import com.jet.align.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private static final int MIN_MONTHS_FLOOR = 3;
    private static final int DEFAULT_WINDOW_MONTHS = 12;
    private static final int MAX_SPAN_MONTHS = 36;
    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found with id: ";

    private final ZoneId timezone;
    private final TransactionRepository repository;
    private final TransactionMapper mapper;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  TransactionMapper mapper,
                                  @Value("${align.timezone}") String timezone) {
        this.repository = transactionRepository;
        this.mapper = mapper;
        this.timezone = ZoneId.of(timezone);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, User user) {
        Transaction  transaction = mapper.toTransaction(request);
        transaction.setType(request.category().getType());
        transaction.setUser(user);
        return mapper.toResponse(repository.save(transaction));
    }

    @Override
    @Transactional
    public TransactionResponse getTransactionById(UUID id, User user) {
        Transaction  transaction = repository.findByIdAndUser(id, user).orElseThrow(
                () -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));
        return mapper.toResponse(transaction);
    }

    @Override
    @Transactional
    public Page<TransactionResponse> getTransactions(User user, Pageable pageable, TransactionFilter filter) {
        return repository.findAll(TransactionSpecifications.withFilter(user, filter), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(UUID id, TransactionUpdateRequest request, User user) {
        Transaction transaction = repository.findByIdAndUser(id, user).orElseThrow(
                () -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));
        transaction.setType(request.category().getType());
        mapper.updateTransaction(request, transaction);
        return mapper.toResponse(repository.save(transaction));
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID id, User user) {
        Transaction transaction = repository.findByIdAndUser(id, user).orElseThrow(
                () -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));
        repository.delete(transaction);
    }

    @Override
    @Transactional
    public FinancialSummaryResponse getSummary(User user, TransactionFilter filter) {
        List<Transaction> transactions = repository.findAll(TransactionSpecifications.withFilter(user, filter));
        BigDecimal totalIncome = sum(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sum(transactions, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        return new FinancialSummaryResponse(totalIncome, totalExpense, balance);
    }

    @Override
    @Transactional
    public CategoryBreakdownResponse getCategoryBreakdown(User user, LocalDate from, LocalDate to) {
        LocalDate rangeFrom = from != null ? from : YearMonth.now(timezone).atDay(1);
        LocalDate rangeTo = to != null ? to : YearMonth.now(timezone).atEndOfMonth();
        if (rangeFrom.isAfter(rangeTo)) {
            throw new BusinessException("The 'from' date cannot be after the 'to' date.");
        }
        TransactionFilter filter = new TransactionFilter(null, null, rangeFrom, rangeTo);
        List<Transaction> transactions = repository.findAll(TransactionSpecifications.withFilter(user, filter));
        List<CategoryAmount> incomes = breakdown(transactions, TransactionType.INCOME);
        List<CategoryAmount> expenses = breakdown(transactions, TransactionType.EXPENSE);
        return new CategoryBreakdownResponse(expenses, incomes);
    }

    @Override
    @Transactional
    public MonthlySummaryResponse getMonthlySummary(User user, MonthlySummaryFilter filter) {
        YearMonth to = filter.to() != null ? filter.to() : YearMonth.now(timezone);
        YearMonth from = resolveMonth(filter, user, to);
        if (from.isAfter(to)) {
            throw new BusinessException("The 'from' month cannot be after the 'to' month.");
        }
        if (ChronoUnit.MONTHS.between(from, to) >= MAX_SPAN_MONTHS) {
            throw new BusinessException("The span between 'from' and 'to' months cannot exceed " + MAX_SPAN_MONTHS + " months.");
        }
        TransactionFilter rangeFilter = new TransactionFilter(
                filter.type(), filter.category(), from.atDay(1), to.atEndOfMonth());
        List<Transaction> transactions = repository.findAll(TransactionSpecifications.withFilter(user, rangeFilter));
        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())));
        List<MonthlyPoint> months = new ArrayList<>();
        for (YearMonth cursor = from; !cursor.isAfter(to); cursor = cursor.plusMonths(1)) {
            List<Transaction> monthTransactions = byMonth.getOrDefault(cursor, List.of());
            BigDecimal income = sum(monthTransactions, TransactionType.INCOME);
            BigDecimal expense = sum(monthTransactions, TransactionType.EXPENSE);
            months.add(new MonthlyPoint(cursor, income, expense, income.subtract(expense)));
        }
        return new MonthlySummaryResponse(months);
    }

    private List<CategoryAmount> breakdown(List<Transaction> transactions, TransactionType type) {
        Map<Category, BigDecimal> byCategory = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
        BigDecimal total = byCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return byCategory.entrySet().stream()
                .map(e -> new CategoryAmount(e.getKey(), e.getValue(), percentageOf(e.getValue(), total)))
                .sorted(Comparator.comparing(CategoryAmount::amount).reversed())
                .toList();
    }
    private BigDecimal percentageOf(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP);
    }

    private YearMonth resolveMonth(MonthlySummaryFilter filter, User user, YearMonth to) {
        if (filter.from() != null) {
            return filter.from();
        }
        YearMonth earliest = repository.findFirstByUserOrderByDateAsc(user)
                .map(transaction -> YearMonth.from(transaction.getDate()))
                .orElse(to);

        YearMonth defaultWindowStart = to.minusMonths(DEFAULT_WINDOW_MONTHS - 1L);
        YearMonth floorStart = to.minusMonths(MIN_MONTHS_FLOOR - 1L);
        YearMonth candidate = earliest.isAfter(defaultWindowStart) ? earliest : defaultWindowStart;
        return candidate.isAfter(floorStart) ? floorStart : candidate;
    }

    private BigDecimal sum(List<Transaction> transactions, TransactionType type) {
        BigDecimal totalSum = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == type) {
                totalSum = totalSum.add(transaction.getAmount());
            }
        }
        return totalSum;
    }
}
