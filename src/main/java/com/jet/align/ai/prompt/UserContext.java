package com.jet.align.ai.prompt;

import java.time.LocalDate;
import java.util.List;

public record UserContext(
        LocalDate today,
        List<String> memories
) {}
