package com.jet.align.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventRequest(

        @NotBlank(message = "Title is required.")
        @Size(max = 255, message = "Title cannot exceed 255 characters.")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters.")
        String description,

        @NotNull(message = "Start date/time is required.")
        LocalDateTime startAt,

        LocalDateTime endAt,

        @Size(max = 255, message = "Location cannot exceed 255 characters.")
        String location,

        @PositiveOrZero(message = "Reminder minutes must be zero or positive.")
        Integer reminderMinutesBefore

) {}

