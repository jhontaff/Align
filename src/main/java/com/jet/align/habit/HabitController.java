package com.jet.align.habit;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> createHabit(
            @Valid @RequestBody HabitRequest request,
            @AuthenticationPrincipal User user) {
        HabitResponse response = habitService.createHabit(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        "Habit created successfully.",
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HabitResponse>> getHabitById(
            @PathVariable("id") UUID habitId,
            @AuthenticationPrincipal User user) {
        HabitResponse response = habitService.getHabitById(user, habitId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Habit retrieved successfully.",
                        response
                ));
    }

    @GetMapping
    public  ResponseEntity<ApiResponse<List<HabitResponse>>> getHabits(
            @AuthenticationPrincipal User user
    ){
        List<HabitResponse> response = habitService.getHabits(user);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Habits retrieved successfully.",
                        response
                ));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HabitResponse>> updateHabit(
            @PathVariable UUID id,
            @Valid @RequestBody HabitRequest request,
            @AuthenticationPrincipal User user) {
        HabitResponse response = habitService.updateHabit(user, id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Habit updated successfully.",
                        response
                ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        habitService.deleteHabit(user, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Habit deleted successfully.", null));
    }

    @PostMapping("/{id}/completions")
    public ResponseEntity<ApiResponse<HabitResponse>> completeHabit(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        HabitResponse response = habitService.completeHabit(user, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Habit marked as complete.",
                        response
                ));
    }
}
