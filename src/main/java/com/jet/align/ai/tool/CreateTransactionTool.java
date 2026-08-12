package com.jet.align.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jet.align.finance.TransactionService;
import com.jet.align.finance.dto.TransactionRequest;
import com.jet.align.finance.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreateTransactionTool implements  Tool<TransactionResponse> {

    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;

    private static final String PARAMETERS_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "amount": {
                  "type": "number",
                  "description": "The amount of the transaction."
                },
                "category": {
                  "type": "string",
                  "enum": ["FOOD","TRANSPORT", "HOUSING", "HEALTH",  "ENTERTAINMENT", "EDUCATION", "SHOPPING", "UTILITIES", "OTHER_EXPENSE", "SALARY", "FREELANCE", "INVESTMENT", "GIFT", "OTHER_INCOME"],
                  "description": "The category of the transaction."
                },
                "description": {
                  "type": "string",
                  "description": "Optional details about the transaction."
                },
                "date": {
                  "type": "string",
                  "format": "date",
                  "description": "The date of the transaction in ISO-8601 format (YYYY-MM-DD)."
                }
              },
              "required": ["amount", "category"],
              "additionalProperties": false
            }
            """;

    @Override
    public String name() {
        return "create_transaction";
    }

    @Override
    public String description() {
        return "Creates a new transaction for the authenticated user.";
    }

    @Override
    public Map<String, Object> parameters() {
        try {
            return objectMapper.readValue(
                    PARAMETERS_SCHEMA,
                    new TypeReference<>() {}
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Invalid JSON Schema for tool " + name(), e);
        }
    }

    @Override
    public ToolResult<TransactionResponse> execute(ToolContext context) {
        TransactionRequest request =
                objectMapper.convertValue(
                        context.arguments(),
                        TransactionRequest.class
                );
        TransactionResponse response =
                transactionService.createTransaction(
                        request,
                        context.user()
                );
        return new ToolResult<>(
                response,
                "Transaction created successfully."
        );
    }
}
