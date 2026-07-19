package com.jet.align.auth;

import com.jet.align.auth.dto.AuthResponse;
import com.jet.align.auth.dto.LoginRequest;
import com.jet.align.auth.dto.RegisterRequest;


public interface AuthService {

    public AuthResponse register(RegisterRequest request);

    public AuthResponse login(LoginRequest request);

}
