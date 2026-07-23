package com.jet.align.ai.agent.execution;

import com.jet.align.ai.model.ToolCall;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.user.User;

public interface ToolExecutionService {

    ToolResult<?> execute(
            ToolCall toolCall,
            User user
    );

}
