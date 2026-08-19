package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.finance.TransactionService;
import com.jet.align.finance.dto.TransactionFilter;
import com.jet.align.finance.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListTransactionsTool implements  Tool<List<TransactionResponse>> {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
              "type": {
                  "type": "string",
                  "enum": ["INCOME", "EXPENSE"],
                  "description": "Optional filter for the type of transactions."
                },
                "category": {
                  "type": "string",
                  "enum": ["FOOD","TRANSPORT", "HOUSING", "HEALTH",  "ENTERTAINMENT", "EDUCATION", "SHOPPING", "UTILITIES", "OTHER_EXPENSE", "SALARY", "FREELANCE", "INVESTMENT", "GIFT", "OTHER_INCOME"],
                  "description": "Optional filter for the category of transactions."
                },
                "from": {
                  "type": "string",
                  "format": "date",
                  "description": "Optional start date for filtering transactions (inclusive)."
                },
                "to": {
                  "type": "string",
                  "format": "date",
                  "description": "Optional end date for filtering transactions (inclusive)."
                }
              },
               "required": [],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "list_transactions";
    }

    @Override
    public String description() {
        return "Lists the authenticated user's transactions, optionally filtered by type, category, or date range.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(
                    PARAMETERS_SCHEMA,
                    new TypeReference<>() {}
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON Schema for tool " + name(), e);
        }

    }

    @Override
    public ToolResult<List<TransactionResponse>> execute(ToolContext context) {
        TransactionFilter filter = objectMapper.convertValue(
                context.arguments(),
                TransactionFilter.class
        );

        Pageable pageable = PageRequest.of(0,20, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<TransactionResponse> transactions = transactionService.getTransactions(
                context.user(),
                pageable,
                filter
        ).getContent();

        return new ToolResult<>(transactions, "Transactions retrieved successfully");


    }
}
