package com.jet.align.habit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HabitRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name cannot exceed 100 characters.")
        String name
) {}
