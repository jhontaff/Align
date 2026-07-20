package com.jet.align.auth;

import com.jet.align.auth.dto.AuthResponse;
import com.jet.align.auth.dto.LoginRequest;
import com.jet.align.auth.dto.RegisterRequest;
import com.jet.align.common.response.ApiResponse;
import com.jet.align.user.User;
import com.jet.align.user.UserMapper;
import com.jet.align.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal User user
    ) {

        UserResponse userResponse = userMapper.toResponse(user);
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Authenticated user.",
                        userResponse
                )
        );
    }


}
