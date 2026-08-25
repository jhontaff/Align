package com.jet.align.ai.tool;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
