package com.jet.align.habit.dto;

import java.time.Instant;
import java.util.UUID;

public record HabitResponse(
        UUID id,
        String name,
        int currentStreak,
        int longestStreak,
        Instant createdAt,
        Instant updatedAt
) {}

