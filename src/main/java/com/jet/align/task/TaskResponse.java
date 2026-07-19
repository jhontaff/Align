package com.jet.align.task;

import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(

        UUID id,
        String title,
        String description,
        TaskStatus taskStatus,
        Priority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt

) {}
