package com.jet.align.ai.agent.execution;

import com.jet.align.ai.llm.ToolCall;
import com.jet.align.ai.tool.*;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToolExecutionServiceImpl implements ToolExecutionService {

    private final ToolRegistry toolRegistry;
    private final PendingActionService pendingActionService;

    @Override
    public ToolResult<?> execute(ToolCall toolCall, User user) {
        Tool<?> tool = toolRegistry.get(toolCall.name())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown tool: " + toolCall.name()));

        if (tool.risk() != RiskLevel.SAFE) {
            PendingAction pending = pendingActionService.create(user, tool.name(), toolCall.arguments());
            return new ToolResult<>(
                    Map.of("pendingActionId", pending.getId().toString()),
                    "This action requires explicit user confirmation before it executes. Tell the user exactly what you're "
                            + "about to do, and give them this confirmation id so they can approve it through the app: " + pending.getId()
                            + ". You cannot confirm it on their behalf in this chat."
            );
        }

        return tool.execute(new ToolContext(user, toolCall.arguments()));
    }

}
