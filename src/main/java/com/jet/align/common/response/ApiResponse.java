package com.jet.align.common.response;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiResponse<T>(
        Instant timestamp,
        int status,
        boolean success,
        String message,
        T data,
        Object errors
) {

    public static <T> ApiResponse<T> success(HttpStatus status,
                                             String message,
                                             T data) {

        return new ApiResponse<>(
                Instant.now(),
                status.value(),
                true,
                message,
                data,
                null
        );
    }

    public static <T> ApiResponse<T> error(HttpStatus status,
                                           String message,
                                           Object errors) {

        return new ApiResponse<>(
                Instant.now(),
                status.value(),
                false,
                message,
                null,
                errors
        );
    }

    public static <T> ApiResponse<T> error(HttpStatus status,
                                           String message) {

        return error(status, message, null);
    }
}
