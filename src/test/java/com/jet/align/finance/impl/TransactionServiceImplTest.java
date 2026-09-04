package com.jet.align.finance.impl;

import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.finance.Transaction;
import com.jet.align.finance.TransactionMapper;
import com.jet.align.finance.TransactionRepository;
import com.jet.align.finance.dto.FinancialSummaryResponse;
import com.jet.align.finance.dto.MonthlyPoint;
import com.jet.align.finance.dto.MonthlySummaryFilter;
import com.jet.align.finance.dto.MonthlySummaryResponse;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceImplTest {

    private final TransactionRepository repository = mock(TransactionRepository.class);
    private final TransactionMapper mapper = mock(TransactionMapper.class);
    private final TransactionServiceImpl service = new TransactionServiceImpl(repository, mapper, "UTC");
    private final User user = new User();

    private TransactionResponse sampleResponse(UUID id) {
        return new TransactionResponse(
                id, TransactionType.EXPENSE, BigDecimal.TEN, Category.FOOD,
                "Almuerzo", LocalDate.of(2026, 8, 1), Instant.now(), Instant.now());
    }

    private Transaction transactionOf(TransactionType type, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setAmount(amount);
        return transaction;
    }

    // TransactionRequest no tiene un campo "type": Category es la única fuente de
    // verdad (Category.FOOD trae consigo TransactionType.EXPENSE). El service debe
    // derivar el type de la category recién mapeada, nunca confiar en uno externo.
    @Test
    void al_crear_una_transaccion_el_type_se_deriva_de_la_category_no_del_request() {
        TransactionRequest request = new TransactionRequest(
                BigDecimal.TEN, Category.FOOD, "Almuerzo", LocalDate.of(2026, 8, 1));
        Transaction mapped = new Transaction();
        TransactionResponse expected = sampleResponse(UUID.randomUUID());

        when(mapper.toTransaction(request)).thenReturn(mapped);
        when(repository.save(mapped)).thenReturn(mapped);
        when(mapper.toResponse(mapped)).thenReturn(expected);

        TransactionResponse response = service.createTransaction(request, user);

        assertThat(mapped.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(mapped.getUser()).isEqualTo(user);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void getTransactionById_devuelve_la_transaccion_mapeada_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Transaction transaction = new Transaction();
        TransactionResponse expected = sampleResponse(id);
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(transaction));
        when(mapper.toResponse(transaction)).thenReturn(expected);

        TransactionResponse response = service.getTransactionById(id, user);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void getTransactionById_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransactionById(id, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Igual que en create: si la category cambia de una de gasto a una de ingreso
    // (o viceversa), el type debe seguir a la nueva category, no quedar pisado por
    // el valor que la transacción ya tenía guardado.
    @Test
    void al_actualizar_una_transaccion_el_type_se_re_deriva_de_la_nueva_category() {
        UUID id = UUID.randomUUID();
        Transaction existing = transactionOf(TransactionType.EXPENSE, BigDecimal.TEN);
        existing.setCategory(Category.FOOD);
        TransactionUpdateRequest request = new TransactionUpdateRequest(
                BigDecimal.valueOf(500), Category.SALARY, "Cambio a ingreso", LocalDate.of(2026, 8, 2));
        TransactionResponse expected = sampleResponse(id);

        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(expected);

        TransactionResponse response = service.updateTransaction(id, request, user);

        assertThat(existing.getType()).isEqualTo(TransactionType.INCOME);
        verify(mapper).updateTransaction(request, existing);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void updateTransaction_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        TransactionUpdateRequest request = new TransactionUpdateRequest(
                BigDecimal.TEN, Category.FOOD, "x", LocalDate.of(2026, 8, 1));
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTransaction(id, request, user))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteTransaction_elimina_la_transaccion_cuando_pertenece_al_usuario() {
        UUID id = UUID.randomUUID();
        Transaction transaction = new Transaction();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.of(transaction));

        service.deleteTransaction(id, user);

        verify(repository).delete(transaction);
    }

    @Test
    void deleteTransaction_lanza_ResourceNotFoundException_si_no_existe_o_no_es_del_usuario() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTransaction(id, user))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any(Transaction.class));
    }

    @Test
    void getSummary_suma_income_y_expense_por_separado_y_calcula_el_balance() {
        Transaction expense1 = transactionOf(TransactionType.EXPENSE, BigDecimal.valueOf(100));
        Transaction expense2 = transactionOf(TransactionType.EXPENSE, BigDecimal.valueOf(50));
        Transaction income = transactionOf(TransactionType.INCOME, BigDecimal.valueOf(300));
        TransactionFilter filter = new TransactionFilter(null, null, null, null);

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(expense1, expense2, income));

        FinancialSummaryResponse summary = service.getSummary(user, filter);

        assertThat(summary.totalExpense()).isEqualByComparingTo("150");
        assertThat(summary.totalIncome()).isEqualByComparingTo("300");
        assertThat(summary.balance()).isEqualByComparingTo("150");
    }

    @Test
    void getTransactions_delega_en_el_repository_con_specification_y_pageable_y_mapea_cada_resultado() {
        Transaction transaction = new Transaction();
        TransactionResponse expected = sampleResponse(UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 20);
        TransactionFilter filter = new TransactionFilter(null, null, null, null);
        Page<Transaction> page = new PageImpl<>(List.of(transaction));

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(transaction)).thenReturn(expected);

        Page<TransactionResponse> response = service.getTransactions(user, pageable, filter);

        assertThat(response.getContent()).containsExactly(expected);
    }

    // getMonthlySummary: ventana adaptativa por defecto (sin from/to en el filtro)

    @Test
    void getMonthlySummary_usa_ventana_de_doce_meses_cuando_el_usuario_tiene_un_anio_de_historia() {
        YearMonth to = YearMonth.now(ZoneId.of("UTC"));
        YearMonth from = to.minusMonths(11);
        Transaction firstTransaction = transactionOf(TransactionType.INCOME, BigDecimal.TEN);
        firstTransaction.setDate(from.minusMonths(1).atDay(15)); // más vieja que la ventana de 12 meses
        MonthlySummaryFilter filter = new MonthlySummaryFilter(null, null, null, null);

        when(repository.findFirstByUserOrderByDateAsc(user)).thenReturn(Optional.of(firstTransaction));
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        MonthlySummaryResponse response = service.getMonthlySummary(user, filter);

        assertThat(response.months()).hasSize(12);
        assertThat(response.months().get(0).month()).isEqualTo(from);
        assertThat(response.months().get(11).month()).isEqualTo(to);
    }

    @Test
    void getMonthlySummary_usuario_nuevo_respeta_el_piso_minimo_de_tres_meses() {
        YearMonth to = YearMonth.now(ZoneId.of("UTC"));
        Transaction firstTransaction = transactionOf(TransactionType.EXPENSE, BigDecimal.ONE);
        firstTransaction.setDate(to.atDay(5)); // se registró este mismo mes
        MonthlySummaryFilter filter = new MonthlySummaryFilter(null, null, null, null);

        when(repository.findFirstByUserOrderByDateAsc(user)).thenReturn(Optional.of(firstTransaction));
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        MonthlySummaryResponse response = service.getMonthlySummary(user, filter);

        assertThat(response.months()).hasSize(3);
        assertThat(response.months().get(0).month()).isEqualTo(to.minusMonths(2));
        assertThat(response.months().get(2).month()).isEqualTo(to);
    }

    @Test
    void getMonthlySummary_sin_transacciones_devuelve_piso_minimo_todo_en_cero() {
        MonthlySummaryFilter filter = new MonthlySummaryFilter(null, null, null, null);
        when(repository.findFirstByUserOrderByDateAsc(user)).thenReturn(Optional.empty());
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        MonthlySummaryResponse response = service.getMonthlySummary(user, filter);

        assertThat(response.months()).hasSize(3);
        assertThat(response.months()).allSatisfy(point -> {
            assertThat(point.income()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(point.expense()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(point.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    // getMonthlySummary: rango explícito (from/to provistos por el caller)

    @Test
    void getMonthlySummary_con_from_explicito_no_consulta_la_primera_transaccion_del_usuario() {
        YearMonth to = YearMonth.now(ZoneId.of("UTC"));
        YearMonth from = to.minusMonths(2);
        MonthlySummaryFilter filter = new MonthlySummaryFilter(from, to, null, null);

        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        service.getMonthlySummary(user, filter);

        verify(repository, never()).findFirstByUserOrderByDateAsc(any(User.class));
    }

    @Test
    void getMonthlySummary_rellena_con_ceros_el_mes_sin_transacciones_en_medio_del_rango() {
        YearMonth to = YearMonth.now(ZoneId.of("UTC"));
        YearMonth from = to.minusMonths(2);
        YearMonth gapMonth = from.plusMonths(1);
        Transaction income = transactionOf(TransactionType.INCOME, BigDecimal.valueOf(500));
        income.setDate(from.atDay(1));
        Transaction expense = transactionOf(TransactionType.EXPENSE, BigDecimal.valueOf(200));
        expense.setDate(to.atDay(1));
        MonthlySummaryFilter filter = new MonthlySummaryFilter(from, to, null, null);

        when(repository.findAll(any(Specification.class))).thenReturn(List.of(income, expense));

        MonthlySummaryResponse response = service.getMonthlySummary(user, filter);

        assertThat(response.months()).hasSize(3);
        MonthlyPoint gapPoint = response.months().stream()
                .filter(p -> p.month().equals(gapMonth))
                .findFirst().orElseThrow();
        assertThat(gapPoint.income()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(gapPoint.expense()).isEqualByComparingTo(BigDecimal.ZERO);

        MonthlyPoint fromPoint = response.months().get(0);
        assertThat(fromPoint.income()).isEqualByComparingTo("500");
        assertThat(fromPoint.balance()).isEqualByComparingTo("500");

        MonthlyPoint toPoint = response.months().get(2);
        assertThat(toPoint.expense()).isEqualByComparingTo("200");
        assertThat(toPoint.balance()).isEqualByComparingTo("-200");
    }

    @Test
    void getMonthlySummary_lanza_BusinessException_si_from_es_posterior_a_to() {
        YearMonth to = YearMonth.now(ZoneId.of("UTC"));
        MonthlySummaryFilter filter = new MonthlySummaryFilter(to.plusMonths(1), to, null, null);

        assertThatThrownBy(() -> service.getMonthlySummary(user, filter))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getMonthlySummary_lanza_BusinessException_si_el_rango_excede_el_maximo_permitido() {
        YearMonth to = YearMonth.now(ZoneId.of("UTC"));
        YearMonth from = to.minusMonths(36); // MAX_SPAN_MONTHS = 36, límite exclusivo
        MonthlySummaryFilter filter = new MonthlySummaryFilter(from, to, null, null);

        assertThatThrownBy(() -> service.getMonthlySummary(user, filter))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).findAll(any(Specification.class));
    }
}
