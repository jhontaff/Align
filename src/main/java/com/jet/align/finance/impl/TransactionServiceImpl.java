package com.jet.align.finance.impl;

import com.jet.align.finance.TransactionService;
import com.jet.align.finance.dto.*;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {
    @Override
    public TransactionResponse createTransaction(TransactionRequest request, User user) {
        return null;
    }

    @Override
    public TransactionResponse getTransactionById(UUID id, User user) {
        return null;
    }

    @Override
    public Page<TransactionResponse> getTransactions(User user, Pageable pageable, TransactionFilter filter) {
        return null;
    }

    @Override
    public TransactionResponse updateTransaction(UUID id, TransactionUpdateRequest request, User user) {
        return null;
    }

    @Override
    public void deleteTransaction(UUID id, User user) {

    }

    @Override
    public FinancialSummaryResponse getSummary(User user, TransactionFilter filter) {
        return null;
    }
}
