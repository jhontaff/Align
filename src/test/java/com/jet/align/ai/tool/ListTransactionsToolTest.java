package com.jet.align.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.finance.TransactionService;
import com.jet.align.finance.dto.TransactionFilter;
import com.jet.align.finance.dto.TransactionResponse;
import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListTransactionsToolTest {

    // A diferencia de UpdateTaskTool, acá no hay merge parcial contra un estado
    // actual: TransactionFilter se arma de una sola vez con convertValue, igual que
    // un TaskRequest en CreateTaskTool. Es el sub-caso que documentamos en el
    // CLAUDE.md: filtro con varios campos opcionales, sin merge.

    private final TransactionService transactionService = mock(TransactionService.class);
    private final ListTransactionsTool tool = new ListTransactionsTool(
            transactionService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();

    private TransactionResponse transaction(Category category) {
        return new TransactionResponse(
                UUID.randomUUID(), category.getType(), BigDecimal.TEN, category,
                "desc", LocalDate.of(2026, 8, 1), Instant.now(), Instant.now());
    }

    @Test
    void sin_filtros_en_los_argumentos_lista_transacciones_sin_filtrar() {
        List<TransactionResponse> transactions = List.of(transaction(Category.FOOD), transaction(Category.SALARY));
        TransactionFilter emptyFilter = new TransactionFilter(null, null, null, null);
        when(transactionService.getTransactions(eq(user), any(Pageable.class), eq(emptyFilter)))
                .thenReturn(new PageImpl<>(transactions));

        ToolResult<List<TransactionResponse>> result = tool.execute(new ToolContext(user, Map.of()));

        // Igual que list_tasks: el payload es el contenido de la página, no el Page
        // envuelto con metadata de paginación.
        assertThat(result.payload()).containsExactlyElementsOf(transactions);
    }

    @Test
    void filtros_recibidos_como_strings_crudos_se_convierten_a_TransactionFilter() {
        List<TransactionResponse> filtered = List.of(transaction(Category.FOOD));
        when(transactionService.getTransactions(eq(user), any(Pageable.class), any(TransactionFilter.class)))
                .thenReturn(new PageImpl<>(filtered));

        // Los 4 campos llegan como String crudo, tal como los arma el LLM a partir
        // del JSON schema -- nunca como TransactionType/Category/LocalDate ya
        // instanciados.
        ToolContext context = new ToolContext(user, Map.of(
                "type", "EXPENSE",
                "category", "FOOD",
                "from", "2026-08-01",
                "to", "2026-08-10"));

        ToolResult<List<TransactionResponse>> result = tool.execute(context);

        ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(transactionService).getTransactions(eq(user), any(Pageable.class), captor.capture());
        TransactionFilter filter = captor.getValue();

        assertThat(filter.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(filter.category()).isEqualTo(Category.FOOD);
        assertThat(filter.from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(filter.to()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(result.payload()).containsExactlyElementsOf(filtered);
    }

    @Test
    void usa_el_mismo_tamano_y_orden_de_pagina_por_defecto_que_list_tasks() {
        TransactionFilter emptyFilter = new TransactionFilter(null, null, null, null);
        when(transactionService.getTransactions(eq(user), any(Pageable.class), eq(emptyFilter)))
                .thenReturn(new PageImpl<>(List.of()));

        tool.execute(new ToolContext(user, Map.of()));

        // El LLM no controla paginación (mismo criterio YAGNI que list_tasks). Este
        // test guarda ese contrato ante un cambio accidental del default.
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionService).getTransactions(eq(user), captor.capture(), eq(emptyFilter));
        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
