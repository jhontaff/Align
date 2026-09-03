package com.jet.align.calendar.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location,
        Integer reminderMinutesBefore,
        Instant createdAt,
        Instant updatedAt
) {}

