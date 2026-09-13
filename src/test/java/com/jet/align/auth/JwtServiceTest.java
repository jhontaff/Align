package com.jet.align.auth;

import com.jet.align.user.Role;
import com.jet.align.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-not-a-real-secret-0000000000000000000000000000000000";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

    private static User userWithId(UUID id) {
        User user = User.builder().email("jhon@align.dev").role(Role.USER).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void generateToken_usa_el_id_como_subject_y_extractUserId_lo_recupera() {
        UUID id = UUID.randomUUID();

        String token = jwtService.generateToken(userWithId(id));

        assertThat(jwtService.extractUserId(token)).isEqualTo(id);
    }

    @Test
    void extractUserId_trata_un_token_con_email_en_el_subject_como_JwtException() {
        // Formato anterior a que el subject fuera el id: debe caer en el mismo camino que
        // cualquier token inválido (401), no en un IllegalArgumentException (500).
        String legacyToken = Jwts.builder()
                .subject("jhon@align.dev")
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(legacyToken))
                .isInstanceOf(MalformedJwtException.class)
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUserId_lanza_JwtException_si_el_token_esta_vencido() {
        JwtService alreadyExpired = new JwtService(SECRET, Duration.ofSeconds(-60));
        String token = alreadyExpired.generateToken(userWithId(UUID.randomUUID()));

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void extractUserId_lanza_JwtException_si_la_firma_no_coincide() {
        JwtService otherKey = new JwtService("another-secret-that-is-long-enough-000000000000000000000", Duration.ofHours(1));
        String token = otherKey.generateToken(userWithId(UUID.randomUUID()));

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }
}
