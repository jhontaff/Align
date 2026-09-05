package com.jet.align.finance;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.finance.dto.DailyAmount;
import com.jet.align.finance.dto.FinancialSummaryResponse;
import com.jet.align.finance.dto.MonthlyChartResponse;
import com.jet.align.finance.dto.MonthlySeries;
import com.jet.align.finance.dto.TransactionFilter;
import com.jet.align.finance.dto.TransactionRequest;
import com.jet.align.finance.dto.TransactionResponse;
import com.jet.align.finance.dto.TransactionUpdateRequest;
import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionControllerTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final TransactionController controller = new TransactionController(transactionService);
    private final User user = new User();

    private TransactionResponse sampleResponse(UUID id) {
        return new TransactionResponse(
                id, TransactionType.EXPENSE, BigDecimal.TEN, Category.FOOD,
                "Almuerzo", LocalDate.of(2026, 8, 1), Instant.now(), Instant.now());
    }

    @Test
    void createTransaction_devuelve_201_con_la_transaccion_creada_por_el_service() {
        TransactionRequest request = new TransactionRequest(
                BigDecimal.TEN, Category.FOOD, "Almuerzo", LocalDate.of(2026, 8, 1));
        TransactionResponse expected = sampleResponse(UUID.randomUUID());
        when(transactionService.createTransaction(request, user)).thenReturn(expected);

        ResponseEntity<ApiResponse<TransactionResponse>> response =
                controller.createTransaction(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void getTransactionById_devuelve_200_con_la_transaccion_del_service() {
        UUID id = UUID.randomUUID();
        TransactionResponse expected = sampleResponse(id);
        when(transactionService.getTransactionById(id, user)).thenReturn(expected);

        ResponseEntity<ApiResponse<TransactionResponse>> response =
                controller.getTransactionById(id, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void getTransactions_pasa_el_filtro_y_el_pageable_tal_cual_al_service() {
        TransactionFilter filter = new TransactionFilter(null, Category.FOOD, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<TransactionResponse> expected = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID())));
        when(transactionService.getTransactions(user, pageable, filter)).thenReturn(expected);

        ResponseEntity<ApiResponse<Page<TransactionResponse>>> response =
                controller.getTransactions(user, filter, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void updateTransaction_devuelve_200_con_la_transaccion_actualizada() {
        UUID id = UUID.randomUUID();
        TransactionUpdateRequest request = new TransactionUpdateRequest(
                BigDecimal.TEN, Category.SALARY, "Cambio", LocalDate.of(2026, 8, 2));
        TransactionResponse expected = sampleResponse(id);
        when(transactionService.updateTransaction(id, request, user)).thenReturn(expected);

        ResponseEntity<ApiResponse<TransactionResponse>> response =
                controller.updateTransaction(id, request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void deleteTransaction_delega_en_el_service_y_devuelve_200_sin_body() {
        UUID id = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> response = controller.deleteTransaction(id, user);

        verify(transactionService).deleteTransaction(id, user);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void getTransactionSummary_devuelve_200_con_el_resumen_del_service() {
        TransactionFilter filter = new TransactionFilter(null, null, null, null);
        FinancialSummaryResponse expected = new FinancialSummaryResponse(
                BigDecimal.valueOf(300), BigDecimal.valueOf(150), BigDecimal.valueOf(150));
        when(transactionService.getSummary(user, filter)).thenReturn(expected);

        ResponseEntity<ApiResponse<FinancialSummaryResponse>> response =
                controller.getTransactionSummary(user, filter);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    void getMonthlyChart_devuelve_200_con_la_data_del_service() {
        MonthlyChartResponse expected = new MonthlyChartResponse(
                new MonthlySeries(YearMonth.of(2026, 9),
                        List.of(new DailyAmount(LocalDate.of(2026, 9, 1), BigDecimal.TEN, BigDecimal.ZERO))),
                new MonthlySeries(YearMonth.of(2026, 8), List.of()));
        when(transactionService.getMonthlyChart(user)).thenReturn(expected);

        ResponseEntity<ApiResponse<MonthlyChartResponse>> response = controller.getMonthlyChart(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expected);
    }
}
