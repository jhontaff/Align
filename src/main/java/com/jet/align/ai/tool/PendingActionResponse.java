package com.jet.align.ai.tool;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PendingActionResponse(

        UUID id,
        String toolName,
        Map<String, Object> arguments,
        Instant createdAt)
{}
