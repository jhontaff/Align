package com.jet.align.auth;

import com.jet.align.auth.dto.AuthResponse;
import com.jet.align.auth.dto.ForgotPasswordRequest;
import com.jet.align.auth.dto.LoginRequest;
import com.jet.align.auth.dto.RegisterRequest;
import com.jet.align.auth.dto.ResetPasswordRequest;


public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void requestPasswordReset(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

}
