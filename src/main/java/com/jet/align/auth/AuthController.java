package com.jet.align.auth;

import com.jet.align.auth.dto.AuthResponse;
import com.jet.align.auth.dto.LoginRequest;
import com.jet.align.auth.dto.RegisterRequest;
import com.jet.align.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid  @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "User registered successfully.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid  @RequestBody LoginRequest request){
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.CREATED, "Login successful.", response)
        );
    }

}
