package com.jet.align.task.dto;

import com.jet.align.task.enums.TaskStatus;

import java.time.LocalDate;

public record TaskFilter(
        TaskStatus status,
        LocalDate dueFrom,
        LocalDate dueTo
) {}
