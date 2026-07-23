package com.jet.align.ai.llm;

import com.jet.align.ai.model.ToolCall;

public record LlmResponse(

        String message,

        ToolCall toolCall

) {}