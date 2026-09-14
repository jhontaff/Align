package com.jet.align.auth.impl;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genera y hashea los tokens de recuperación de contraseña.
 *
 * <p>El hash es SHA-256 y no {@code PasswordEncoder} (bcrypt) a propósito:
 * bcrypt saltea distinto en cada {@code encode()}, así que dos hashes del
 * mismo valor no coinciden y no se puede buscar por {@code token_hash = ?}.
 * Un hash determinístico es correcto acá porque el token es aleatorio de alta
 * entropía (32 bytes de SecureRandom), no una contraseña elegida por una
 * persona: el motivo para encarecer el hash con bcrypt no aplica.
 *
 * <p>Package-private, mismo criterio que {@code ApiKeyCipher}: es un detalle
 * de implementación del servicio de auth, no una utilidad transversal.
 */
@Component
class PasswordResetTokenGenerator {

    private static final int TOKEN_LENGTH_BYTES = 32;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final SecureRandom random = new SecureRandom();

    String generateRawToken() {
        byte[] bytes = new byte[TOKEN_LENGTH_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " no disponible en esta JVM.", e);
        }
    }
}
