package com.jet.align.auth.impl;

import com.jet.align.auth.JwtService;
import com.jet.align.auth.PasswordResetToken;
import com.jet.align.auth.PasswordResetTokenRepository;
import com.jet.align.auth.dto.ForgotPasswordRequest;
import com.jet.align.auth.dto.ResetPasswordRequest;
import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.EmailDeliveryException;
import com.jet.align.email.EmailSender;
import com.jet.align.user.User;
import com.jet.align.user.UserMapper;
import com.jet.align.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre solo el flujo de recuperación de contraseña. register/login siguen sin
 * test unitario propio (gap previo a esta feature, no introducido por ella).
 */
class AuthServiceImplTest {

    private static final long TTL_MINUTES = 30;
    private static final String FRONTEND = "http://localhost:4200";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    // Generador real, no mock: el hash es determinista y así el test prueba de
    // punta a punta que el token del link es el que después se encuentra.
    private final PasswordResetTokenGenerator generator = new PasswordResetTokenGenerator();

    private final AuthServiceImpl service = new AuthServiceImpl(
            userRepository,
            passwordEncoder,
            mock(AuthenticationManager.class),
            mock(JwtService.class),
            mock(UserMapper.class),
            tokenRepository,
            generator,
            emailSender,
            TTL_MINUTES,
            FRONTEND);

    private final User user = User.builder().email("ana@example.com").password("old-hash").build();

    // --- requestPasswordReset ----------------------------------------------

    @Test
    void requestPasswordReset_con_email_desconocido_no_guarda_ni_envia_nada() {
        when(userRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatCode(() -> service.requestPasswordReset(new ForgotPasswordRequest("nadie@example.com")))
                .doesNotThrowAnyException();

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestPasswordReset_guarda_el_hash_con_ttl_y_manda_el_link_con_el_token_crudo() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(Optional.empty());

        Instant before = Instant.now();
        service.requestPasswordReset(new ForgotPasswordRequest("ana@example.com"));
        Instant after = Instant.now();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("ana@example.com"), anyString(), bodyCaptor.capture());

        String rawToken = extractToken(bodyCaptor.getValue());
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTokenHash()).isEqualTo(generator.hash(rawToken));
        assertThat(saved.getUsedAt()).isNull();
        assertThat(saved.getExpiresAt()).isBetween(
                before.plus(TTL_MINUTES, ChronoUnit.MINUTES),
                after.plus(TTL_MINUTES, ChronoUnit.MINUTES));
        assertThat(bodyCaptor.getValue()).contains(FRONTEND + "/reset-password?token=" + rawToken);
    }

    @Test
    void requestPasswordReset_reutiliza_la_fila_del_token_activo_anterior_en_vez_de_borrarla() {
        PasswordResetToken previous = activeToken("viejo", Instant.now().plus(5, ChronoUnit.MINUTES));
        String previousHash = previous.getTokenHash();
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(Optional.of(previous));

        service.requestPasswordReset(new ForgotPasswordRequest("ana@example.com"));

        // Misma instancia, hash nuevo: un UPDATE sobre la fila activa. Un
        // delete + insert pegaría contra el índice único parcial, porque
        // Hibernate flushea los INSERT antes que los DELETE.
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isSameAs(previous);
        assertThat(previous.getTokenHash()).isNotEqualTo(previousHash);
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void requestPasswordReset_no_propaga_un_fallo_del_envio_de_email() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(Optional.empty());
        doThrow(new EmailDeliveryException("caído", null))
                .when(emailSender).send(anyString(), anyString(), anyString());

        // Un 500 solo cuando el email existe sería un oráculo de existencia.
        assertThatCode(() -> service.requestPasswordReset(new ForgotPasswordRequest("ana@example.com")))
                .doesNotThrowAnyException();
    }

    // --- resetPassword -----------------------------------------------------

    @Test
    void resetPassword_con_token_valido_cambia_la_contrasena_y_marca_el_token_usado() {
        PasswordResetToken token = activeToken("raw-token", Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(generator.hash("raw-token"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassw0rd")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequest("raw-token", "NewPassw0rd", "NewPassw0rd"));

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_con_token_inexistente_falla_sin_tocar_la_contrasena() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("nope", "NewPassw0rd", "NewPassw0rd")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El enlace no es válido o ya expiró.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_con_token_vencido_falla_con_el_mismo_mensaje_generico() {
        PasswordResetToken token = activeToken("raw-token", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(generator.hash("raw-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("raw-token", "NewPassw0rd", "NewPassw0rd")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El enlace no es válido o ya expiró.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_con_token_ya_usado_falla_y_no_se_puede_reusar() {
        PasswordResetToken token = activeToken("raw-token", Instant.now().plus(10, ChronoUnit.MINUTES));
        token.setUsedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(generator.hash("raw-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("raw-token", "NewPassw0rd", "NewPassw0rd")))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // --- helpers -----------------------------------------------------------

    private PasswordResetToken activeToken(String rawToken, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(generator.hash(rawToken));
        token.setExpiresAt(expiresAt);
        return token;
    }

    private static String extractToken(String emailBody) {
        String marker = "?token=";
        int start = emailBody.indexOf(marker) + marker.length();
        int end = emailBody.indexOf('"', start);
        return emailBody.substring(start, end);
    }
}
