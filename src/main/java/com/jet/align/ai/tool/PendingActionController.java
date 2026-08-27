package com.jet.align.ai.tool;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent/pending-actions")
@RequiredArgsConstructor
public class PendingActionController {

    private  final PendingActionService pendingActionService;

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<?>> confirm(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        ToolResult<?> result = pendingActionService.confirm(user, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result.message(), result.payload()));
    }
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        pendingActionService.reject(user, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Pending action rejected.", null));
    }
    @GetMapping
    public  ResponseEntity<ApiResponse<List<PendingActionResponse>>> getPendingActions(@AuthenticationPrincipal User user) {
        List<PendingActionResponse> response = pendingActionService.list(user);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Pending action found.", response));
    }


}
