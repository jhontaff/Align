package com.jet.align.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.finance.TransactionService;
import com.jet.align.finance.dto.FinancialSummaryResponse;
import com.jet.align.finance.dto.TransactionFilter;
import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetFinancialSummaryToolTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    private final GetFinancialSummaryTool tool = new GetFinancialSummaryTool(
            transactionService, new ObjectMapper().registerModule(new JavaTimeModule()));
    private final User user = new User();

    @Test
    void sin_filtros_delega_en_getSummary_con_un_filtro_vacio() {
        TransactionFilter emptyFilter = new TransactionFilter(null, null, null, null);
        FinancialSummaryResponse expected = new FinancialSummaryResponse(
                BigDecimal.valueOf(300), BigDecimal.valueOf(150), BigDecimal.valueOf(150));
        when(transactionService.getSummary(eq(user), eq(emptyFilter))).thenReturn(expected);

        ToolResult<FinancialSummaryResponse> result = tool.execute(new ToolContext(user, Map.of()));

        assertThat(result.payload()).isEqualTo(expected);
    }

    @Test
    void filtros_recibidos_como_strings_crudos_se_convierten_a_TransactionFilter() {
        FinancialSummaryResponse expected = new FinancialSummaryResponse(
                BigDecimal.ZERO, BigDecimal.valueOf(80), BigDecimal.valueOf(-80));
        when(transactionService.getSummary(eq(user), any(TransactionFilter.class))).thenReturn(expected);

        // Mismos 4 campos, mismo formato crudo que list_transactions -- get_summary
        // reusa el mismo esquema de filtro, solo que sin paginación.
        ToolContext context = new ToolContext(user, Map.of(
                "type", "EXPENSE",
                "category", "FOOD",
                "from", "2026-08-01",
                "to", "2026-08-10"));

        ToolResult<FinancialSummaryResponse> result = tool.execute(context);

        ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(transactionService).getSummary(eq(user), captor.capture());
        TransactionFilter filter = captor.getValue();

        assertThat(filter.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(filter.category()).isEqualTo(Category.FOOD);
        assertThat(filter.from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(filter.to()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(result.payload()).isEqualTo(expected);
    }
}
