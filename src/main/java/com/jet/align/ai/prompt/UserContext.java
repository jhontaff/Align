package com.jet.align.ai.prompt;

import java.time.LocalDateTime;
import java.util.List;

public record UserContext(
        LocalDateTime now,
        List<String> memories
) {}
