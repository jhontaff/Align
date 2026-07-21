package com.jet.align.task.model;

import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskUpdateRequest(

        @NotBlank(message = "Title is required.")
        @Size(max = 100, message = "Title cannot exceed 100 characters.")
        String title,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters.")
        String description,

        @NotNull(message = "Status is required.")
        TaskStatus status,

        @NotNull(message = "Priority is required.")
        Priority priority,

        LocalDate dueDate

) {}