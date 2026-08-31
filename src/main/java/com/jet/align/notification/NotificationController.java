package com.jet.align.notification;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.notification.dto.SubscribeRequest;
import com.jet.align.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PushSubscriptionService pushSubscriptionService;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<ApiResponse<String>> getVapidPublicKey() {
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Vapid public key retrieved successfully.",
                        pushSubscriptionService.getVapidPublicKey())
        );
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @RequestBody SubscribeRequest request,
            @AuthenticationPrincipal User user
    ) {
        pushSubscriptionService.subscribe(user, request);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Subscription registered successfully.", null)
        );
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @RequestParam String endpoint,
            @AuthenticationPrincipal User user
    ) {
        pushSubscriptionService.unsubscribe(user, endpoint);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Subscription removed successfully.", null)
        );
    }
}

