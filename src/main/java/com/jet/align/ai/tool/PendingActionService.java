package com.jet.align.ai.tool;

import com.jet.align.user.User;

import java.util.Map;
import java.util.UUID;

public interface PendingActionService {

    ToolResult<?> confirm(User user, UUID id);
    PendingAction create(User user, String toolName, Map<String, Object> arguments);
    void reject(User user, UUID id);

}
