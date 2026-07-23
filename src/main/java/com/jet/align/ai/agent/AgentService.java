package com.jet.align.ai.agent;

import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolResult;

public interface AgentService {
    ToolResult<?> execute(
            String toolName,
            ToolContext context
    );
}
