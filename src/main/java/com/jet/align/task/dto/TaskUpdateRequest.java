package com.jet.align.task.dto;

import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskUpdateRequest(

        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 100, message = "El título no puede superar los 100 caracteres.")
        String title,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
        String description,

        @NotNull(message = "El estado es obligatorio.")
        TaskStatus status,

        @NotNull(message = "La prioridad es obligatoria.")
        Priority priority,

        @NotNull(message = "La fecha de vencimiento es obligatoria.")
        LocalDate dueDate,
        LocalTime dueTime

) {}