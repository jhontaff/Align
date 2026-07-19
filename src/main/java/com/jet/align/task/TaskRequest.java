package com.jet.align.task;

import com.jet.align.task.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(

            @NotBlank(message = "Title is required.")
            @Size(max = 100, message = "Title cannot exceed 100 characters.")
            String title,

            @Size(max = 1000, message = "Description cannot exceed 1000 characters.")
            String description,

            @NotNull(message = "Priority is required.")
            Priority priority,

            LocalDate dueDate

) {}
