package com.jet.align.habit.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record HabitResponse(
        UUID id,
        String name,
        LocalTime scheduledTime,
        int currentStreak,
        int longestStreak,
        boolean isCompletedToday,
        Instant createdAt,
        Instant updatedAt
) {}
