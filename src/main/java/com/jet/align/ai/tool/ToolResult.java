package com.jet.align.ai.tool;

public record ToolResult<T>(
        T payload,
        String message
) {}