package com.jet.align.auth;


import com.jet.align.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") Duration expiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        // El subject es el id, no el email: la identidad del token debe ser inmutable,
        // y el email es editable desde el perfil.
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        String subject = extractClaim(token, Claims::getSubject);
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            // Tokens emitidos cuando el subject era el email: se tratan como cualquier
            // token inválido (401 vía el filtro), no como un error interno.
            throw new MalformedJwtException("El subject del token no es un id de usuario.", e);
        }
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {

        Claims claims = extractAllClaims(token);

        return resolver.apply(claims);
    }

    public Instant getExpirationInstant() {
        return Instant.now().plus(expiration);
    }
}
