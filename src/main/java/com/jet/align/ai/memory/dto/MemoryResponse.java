package com.jet.align.ai.memory.dto;

import java.time.Instant;
import java.util.UUID;

public record MemoryResponse(
        UUID id,
        String content,
        Instant createdAt
) {}
