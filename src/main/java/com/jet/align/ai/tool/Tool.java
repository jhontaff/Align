package com.jet.align.ai.tool;

import java.util.Map;

public interface Tool<T> {

    String name();

    String description();

    Map<String, Object> parameters();

    ToolResult<T> execute(ToolContext context);
}