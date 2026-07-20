package com.jet.align.auth.impl;

import com.jet.align.auth.AuthService;
import com.jet.align.auth.JwtConstants;
import com.jet.align.auth.JwtService;
import com.jet.align.auth.dto.AuthResponse;
import com.jet.align.auth.dto.LoginRequest;
import com.jet.align.auth.dto.RegisterRequest;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.user.Role;
import com.jet.align.user.User;
import com.jet.align.user.UserMapper;
import com.jet.align.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email is already in use.");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token, JwtConstants.TOKEN_TYPE, jwtService.getExpirationInstant());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = (User) authentication.getPrincipal();

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, JwtConstants.TOKEN_TYPE, jwtService.getExpirationInstant());
    }


}
