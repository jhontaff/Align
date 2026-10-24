package com.jet.align.auth.impl;

import com.jet.align.auth.*;
import com.jet.align.auth.dto.*;
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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
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
    private final SpringTemplateEngine templateEngine;


    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserMapper userMapper,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordResetTokenGenerator passwordResetTokenGenerator,
                           EmailSender emailSender,
                           SpringTemplateEngine templateEngine,
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
        this.templateEngine = templateEngine;
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

        String rawToken = passwordResetTokenGenerator.generateRawToken();

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByUserAndUsedAtIsNull(user)
                .orElseGet(PasswordResetToken::new);
        resetToken.setUser(user);
        resetToken.setTokenHash(passwordResetTokenGenerator.hash(rawToken));
        resetToken.setExpiresAt(Instant.now().plus(passwordResetTtl));
        passwordResetTokenRepository.save(resetToken);

        try {
            emailSender.send(user.getEmail(),
                    "Restablecé tu contraseña de Align",
                    buildResetEmail(user.getFirstName(), rawToken));
        } catch (EmailDeliveryException e) {
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

    private String buildResetEmail(String firstName, String rawToken) {
        Context context = new Context(Locale.forLanguageTag("es"));
        context.setVariable("firstName", firstName);
        context.setVariable("resetLink", frontendBaseUrl + "/reset-password?token=" + rawToken);
        context.setVariable("expiresInMinutes", passwordResetTtl.toMinutes());

        return templateEngine.process("email/reset-password", context);
    }
}
