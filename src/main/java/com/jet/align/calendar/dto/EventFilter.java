package com.jet.align.calendar.dto;

import java.time.LocalDateTime;

public record EventFilter(
        LocalDateTime from,
        LocalDateTime to
) {}
