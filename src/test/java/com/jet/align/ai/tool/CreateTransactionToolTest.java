package com.jet.align.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jet.align.finance.TransactionService;
import com.jet.align.finance.dto.TransactionRequest;
import com.jet.align.finance.dto.TransactionResponse;
import com.jet.align.finance.enums.Category;
import com.jet.align.finance.enums.TransactionType;
import com.jet.align.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateTransactionToolTest {

    private final TransactionService transactionService = mock(TransactionService.class);
    // Spring Boot registra jackson-datatype-jsr310 automáticamente en el ObjectMapper
    // real de la app (mismo motivo que en UpdateTaskToolTest), necesario acá para
    // parsear "date" desde un String ISO-8601.
    private final CreateTransactionTool tool = new CreateTransactionTool(
            new ObjectMapper().registerModule(new JavaTimeModule()), transactionService);
    private final User user = new User();

    @Test
    void execute_convierte_los_argumentos_crudos_del_llm_y_delega_en_createTransaction() {
        TransactionResponse expected = new TransactionResponse(
                UUID.randomUUID(), TransactionType.EXPENSE, BigDecimal.valueOf(45.50),
                Category.FOOD, "Almuerzo", LocalDate.of(2026, 8, 10), Instant.now(), Instant.now());
        when(transactionService.createTransaction(any(TransactionRequest.class), eq(user)))
                .thenReturn(expected);

        // Los argumentos llegan tal como los arma el LLM a partir del JSON schema:
        // amount ya deserializado como número (no String), category/date como
        // Strings crudos.
        ToolContext context = new ToolContext(user, Map.of(
                "amount", 45.50,
                "category", "FOOD",
                "description", "Almuerzo",
                "date", "2026-08-10"));

        ToolResult<TransactionResponse> result = tool.execute(context);

        ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).createTransaction(captor.capture(), eq(user));
        TransactionRequest request = captor.getValue();

        assertThat(request.amount()).isEqualByComparingTo("45.50");
        assertThat(request.category()).isEqualTo(Category.FOOD);
        assertThat(request.description()).isEqualTo("Almuerzo");
        assertThat(request.date()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(result.payload()).isEqualTo(expected);
    }

    // Bug real que corregimos durante la revisión: el schema tenía un campo "type"
    // que TransactionRequest no declara -- category ya implica el type. Si el LLM
    // llegaba a mandarlo, el ObjectMapper real de la app (que falla ante propiedades
    // desconocidas, igual que en UpdateTaskTool) tiraba UnrecognizedPropertyException
    // dentro de execute(). Este test guarda el contrato del schema para que nadie lo
    // reintroduzca sin darse cuenta.
    @Test
    void el_schema_no_expone_type_porque_TransactionRequest_no_tiene_ese_campo() {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) tool.parameters().get("properties");

        assertThat(properties).doesNotContainKey("type");
    }
}
