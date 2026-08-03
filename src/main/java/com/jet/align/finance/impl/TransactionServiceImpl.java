package com.jet.align.finance.impl;

import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.finance.*;
import com.jet.align.finance.dto.*;
import com.jet.align.finance.enums.TransactionType;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.   UUID;

@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found with id: ";
    private final TransactionRepository repository;
    private final TransactionMapper mapper;

    public TransactionServiceImpl(TransactionRepository transactionRepository, TransactionMapper mapper) {
        this.repository = transactionRepository;
        this.mapper = mapper;
    }

    @Override
    public TransactionResponse createTransaction(TransactionRequest request, User user) {
        Transaction  transaction = mapper.toTransaction(request);
        transaction.setType(request.category().getType());
        transaction.setUser(user);
        return mapper.toResponse(repository.save(transaction));
    }

    @Override
    public TransactionResponse getTransactionById(UUID id, User user) {
        Transaction  transaction = repository.findByIdAndUser(id, user).orElseThrow(
                () -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));
        return mapper.toResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getTransactions(User user, Pageable pageable, TransactionFilter filter) {
        return repository.findAll(TransactionSpecifications.withFilter(user, filter), pageable)
                .map(mapper::toResponse);
    }

    @Override
    public TransactionResponse updateTransaction(UUID id, TransactionUpdateRequest request, User user) {
        Transaction transaction = repository.findByIdAndUser(id, user).orElseThrow(
                () -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));
        transaction.setType(request.category().getType());
        mapper.updateTransaction(request, transaction);
        return mapper.toResponse(repository.save(transaction));
    }

    @Override
    public void deleteTransaction(UUID id, User user) {
        Transaction transaction = repository.findByIdAndUser(id, user).orElseThrow(
                () -> new ResourceNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE + id));
        repository.delete(transaction);
    }

    @Override
    public FinancialSummaryResponse getSummary(User user, TransactionFilter filter) {
        List<Transaction> transactions = repository.findAll(TransactionSpecifications.withFilter(user, filter));
        BigDecimal totalIncome = sum(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sum(transactions, TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        return new FinancialSummaryResponse(totalIncome, totalExpense, balance);
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
