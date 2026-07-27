package com.jet.align.ai.llm;

import java.util.List;

public record LlmRequest(
        List<Message> messages,
        List<ToolSpecification> tools
) {

    public LlmRequest {
        messages = List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
