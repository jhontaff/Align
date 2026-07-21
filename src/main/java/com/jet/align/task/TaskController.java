package com.jet.align.task;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.task.model.TaskRequest;
import com.jet.align.task.model.TaskResponse;
import com.jet.align.task.model.TaskUpdateRequest;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user
    ) {

        TaskResponse response = taskService.createTask(request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        "Task created successfully.",
                        response
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable UUID id,
                                                                 @AuthenticationPrincipal User user) {
        TaskResponse response = taskService.getTaskById(id, user);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Task retrieved successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable

    ){
        Page<TaskResponse> response =
                taskService.getTasks(user, pageable, status);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Tasks retrieved successfully.",
                        response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {

        TaskResponse response =
                taskService.updateTask(id, request, user);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Task updated successfully.",
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        taskService.deleteTask(id, user);
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Task deleted successfully.",
                        null
                )
        );
    }

}
