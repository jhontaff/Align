package com.jet.align.finance;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.finance.dto.*;
import com.jet.align.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User user
    ) {
        TransactionResponse response = transactionService.createTransaction(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        "Transaction created successfully.",
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        TransactionResponse response = transactionService.getTransactionById(id, user);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Transaction retrieved successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal User user,
            @ModelAttribute TransactionFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ){
        Page<TransactionResponse> response = transactionService.getTransactions(user, pageable, filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Transactions retrieved successfully.", response));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        TransactionResponse response = transactionService.updateTransaction(id, request, user);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Transaction updated successfully.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        transactionService.deleteTransaction(id, user);
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Transaction deleted successfully.",
                        null));
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal User user,
            @ModelAttribute MonthlySummaryFilter filter
    ) {
        MonthlySummaryResponse response = transactionService.getMonthlySummary(user, filter);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Monthly summary retrieved successfully.", response));
    }

    @GetMapping("/summary/by-category")
    public ResponseEntity<ApiResponse<CategoryBreakdownResponse>> getCategoryBreakdown(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        CategoryBreakdownResponse response = transactionService.getCategoryBreakdown(user, from, to);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Category breakdown retrieved successfully.", response));
    }


    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FinancialSummaryResponse>> getTransactionSummary(
            @AuthenticationPrincipal User user,
            @ModelAttribute TransactionFilter filter
    ) {
        FinancialSummaryResponse response = transactionService.getSummary(user, filter);
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Transaction summary retrieved successfully.",
                        response));
    }
}