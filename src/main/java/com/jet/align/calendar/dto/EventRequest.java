package com.jet.align.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventRequest(

        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 255, message = "El título no puede superar los 255 caracteres.")
        String title,

        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres.")
        String description,

        @NotNull(message = "La fecha y hora de inicio son obligatorias.")
        LocalDateTime startAt,

        LocalDateTime endAt,

        @Size(max = 255, message = "El lugar no puede superar los 255 caracteres.")
        String location,

        @PositiveOrZero(message = "Los minutos de recordatorio deben ser cero o un número positivo.")
        Integer reminderMinutesBefore

) {}

