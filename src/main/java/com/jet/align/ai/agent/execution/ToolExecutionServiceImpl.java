package com.jet.align.ai.agent.execution;

import com.jet.align.ai.model.ToolCall;
import com.jet.align.ai.tool.Tool;
import com.jet.align.ai.tool.ToolContext;
import com.jet.align.ai.tool.ToolRegistry;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ToolExecutionServiceImpl implements ToolExecutionService {

    private final ToolRegistry toolRegistry;

    @Override
    public ToolResult<?> execute(ToolCall toolCall, User user) {
        ToolContext context =
                new ToolContext(
                        user,
                        toolCall.arguments()
                );
        Tool<?> tool =
                toolRegistry.get(toolCall.name()).orElseThrow(
                        () -> new ResourceNotFoundException("Unknown tool: " + toolCall.name())
                );
        return tool.execute(context);
    }
}
