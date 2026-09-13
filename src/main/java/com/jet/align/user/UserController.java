package com.jet.align.user;

import com.jet.align.common.response.ApiResponse;
import com.jet.align.user.dto.AvatarContent;
import com.jet.align.user.dto.EmailUpdateRequest;
import com.jet.align.user.dto.PasswordUpdateRequest;
import com.jet.align.user.dto.ProfileUpdateRequest;
import com.jet.align.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal User user) {
        UserResponse response = userService.getProfile(user);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Profile retrieved successfully.",
                response
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal User user) {
        UserResponse response = userService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Profile updated successfully.",
                response
        ));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordUpdateRequest request,
            @AuthenticationPrincipal User user) {
        userService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Password changed successfully.", null));
    }

    @PutMapping("/me/email")
    public ResponseEntity<ApiResponse<UserResponse>> changeEmail(
            @Valid @RequestBody EmailUpdateRequest request,
            @AuthenticationPrincipal User user) {
        UserResponse response = userService.changeEmail(user, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Email changed successfully.",
                response
        ));
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        // MultipartFile es un tipo de Spring Web: se desarma acá y el service recibe
        // un record neutro, igual que los DTOs del proveedor no salen de ai.llm.gemini.
        userService.saveAvatar(user, new AvatarContent(file.getContentType(), file.getBytes()));
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Avatar uploaded successfully.", null));
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<byte[]> getAvatar(@AuthenticationPrincipal User user) {
        // Único endpoint que no devuelve ApiResponse: sirve la imagen tal cual, con su
        // Content-Type, para que el frontend la use directamente como <img>.
        AvatarContent avatar = userService.getAvatar(user);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .body(avatar.data());
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(@AuthenticationPrincipal User user) {
        userService.deleteAvatar(user);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Avatar removed successfully.", null));
    }
}
