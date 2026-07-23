package com.jet.align.ai.dev;

import com.jet.align.ai.agent.execution.ToolExecutionService;
import com.jet.align.ai.model.ToolCall;
import com.jet.align.ai.tool.ToolResult;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev/tools")
@RequiredArgsConstructor
public class ToolDevController {

    private final ToolExecutionService toolExecutionService;

    @PostMapping("/execute")
    public ToolResult<?> execute(
            @RequestBody ToolCall toolCall,
            @AuthenticationPrincipal User user
    ) {
        return toolExecutionService.execute(toolCall, user);
    }

}

