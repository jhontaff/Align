package com.jet.align.auth.impl;

import com.jet.align.auth.AuthService;
import com.jet.align.auth.JwtConstants;
import com.jet.align.auth.JwtService;
import com.jet.align.auth.PasswordResetToken;
import com.jet.align.auth.PasswordResetTokenRepository;
import com.jet.align.auth.dto.AuthResponse;
import com.jet.align.auth.dto.ForgotPasswordRequest;
import com.jet.align.auth.dto.LoginRequest;
import com.jet.align.auth.dto.RegisterRequest;
import com.jet.align.auth.dto.ResetPasswordRequest;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.EmailDeliveryException;
import com.jet.align.email.EmailSender;
import com.jet.align.user.Role;
import com.jet.align.user.User;
import com.jet.align.user.UserMapper;
import com.jet.align.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenGenerator passwordResetTokenGenerator;
    private final EmailSender emailSender;
    private final Duration passwordResetTtl;
    private final String frontendBaseUrl;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserMapper userMapper,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordResetTokenGenerator passwordResetTokenGenerator,
                           EmailSender emailSender,
                           @Value("${align.password-reset.ttl-minutes}") long passwordResetTtlMinutes,
                           @Value("${align.frontend.base-url}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetTokenGenerator = passwordResetTokenGenerator;
        this.emailSender = emailSender;
        this.passwordResetTtl = Duration.ofMinutes(passwordResetTtlMinutes);
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo electrónico ya está registrado.");
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

    @Override
    public void requestPasswordReset(ForgotPasswordRequest request) {
        Optional<User> found = userRepository.findByEmail(request.email());
        if (found.isEmpty()) {
            // No revelar si el email está registrado: el controller responde
            // lo mismo en ambos casos, y acá simplemente no hay nada que hacer.
            return;
        }
        User user = found.get();

        // Un solo token activo por usuario (el índice único parcial de V20 lo
        // garantiza en la base): el anterior se reemplaza, no conviven dos links.
        passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)
                .ifPresent(passwordResetTokenRepository::delete);

        String rawToken = passwordResetTokenGenerator.generateRawToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(passwordResetTokenGenerator.hash(rawToken));
        resetToken.setExpiresAt(Instant.now().plus(passwordResetTtl));
        passwordResetTokenRepository.save(resetToken);

        try {
            emailSender.send(user.getEmail(), "Restablecé tu contraseña de Align", buildResetEmail(rawToken));
        } catch (EmailDeliveryException e) {
            // Si el envío falla, el request igual termina en 200: un 500 solo
            // para emails registrados sería un oráculo de existencia. El token
            // ya quedó guardado; el usuario puede volver a pedir el reset.
            log.error("No se pudo enviar el email de recuperación al usuario {}", user.getId(), e);
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(passwordResetTokenGenerator.hash(request.token()))
                .filter(token -> !token.isUsed())
                .filter(token -> !token.isExpired(now))
                // Un solo mensaje para "no existe", "ya usado" y "vencido":
                // distinguirlos no ayuda a un usuario legítimo y sí a quien
                // prueba tokens al azar.
                .orElseThrow(() -> new BusinessException("El enlace no es válido o ya expiró."));

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(now);
        passwordResetTokenRepository.save(resetToken);
    }

    private String buildResetEmail(String rawToken) {
        String link = frontendBaseUrl + "/reset-password?token=" + rawToken;
        return "<p>Recibimos una solicitud para restablecer tu contraseña. "
                + "El enlace vence en " + passwordResetTtl.toMinutes() + " minutos.</p>"
                + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                + "<p>Si no fuiste vos, ignorá este mensaje: tu contraseña no cambia.</p>";
    }

}
