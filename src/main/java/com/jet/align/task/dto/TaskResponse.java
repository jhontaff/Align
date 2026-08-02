package com.jet.align.task.dto;

import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TaskResponse(

        UUID id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDate dueDate,
        LocalTime dueTime,
        Instant createdAt,
        Instant updatedAt

) {}
