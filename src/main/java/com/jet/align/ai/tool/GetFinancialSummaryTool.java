package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.finance.TransactionService;
import com.jet.align.finance.dto.FinancialSummaryResponse;
import com.jet.align.finance.dto.TransactionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetFinancialSummaryTool implements Tool<FinancialSummaryResponse> {

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
        return "get_financial_summary";
    }

    @Override
    public String description() {
        return "Returns the total income, total expense, and balance for the authenticated user, optionally filtered by type, category, or date range.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(PARAMETERS_SCHEMA,
                    new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON Schema for tool " + name(), e);
        }
    }

    @Override
    public ToolResult<FinancialSummaryResponse> execute(ToolContext context) {
        TransactionFilter filter = objectMapper.convertValue(
                context.arguments(), TransactionFilter.class);

        FinancialSummaryResponse response = transactionService.getSummary(
                context.user(), filter);
        return new ToolResult<>(response, "Financial summary retrieved successfully.");
    }
}
