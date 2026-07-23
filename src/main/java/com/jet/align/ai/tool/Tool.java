package com.jet.align.ai.tool;

public interface Tool<T> {

    String name();

    String description();

    ToolResult<T> execute(ToolContext context);
}