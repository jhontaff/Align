package com.jet.align.ai.credential.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Cifra y descifra API keys de terceros con AES-GCM, usando una master key
 * global que llega por configuración ({@code align.crypto.secret}).
 *
 * <p>AES-GCM y no AES-CBC porque GCM autentica: si el ciphertext fue alterado,
 * el descifrado falla en vez de devolver basura silenciosamente.
 *
 * <p>El IV se genera al azar por operación y se guarda concatenado adelante del
 * ciphertext (IV + ciphertext, todo en Base64, una sola columna). Reusar el IV
 * en GCM rompe la seguridad del modo, así que nunca es fijo.
 *
 * <p>Package-private a propósito: es un detalle de implementación del servicio
 * de credenciales, no una utilidad transversal. Si algún día aparece un segundo
 * consumidor, ahí se promueve a {@code common}.
 */
@Component
class ApiKeyCipher {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyCipher.class);

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey masterKey;
    private final SecureRandom random = new SecureRandom();

    /**
     * Constructor explícito (no {@code @RequiredArgsConstructor}) porque toma
     * un valor de configuración y los tests lo instancian directo: el mismo
     * idioma que AgentServiceImpl o HabitServiceImpl.
     */
    ApiKeyCipher(@Value("${align.crypto.secret}") String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "align.crypto.secret debe ser Base64 válido.", e);
        }

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException(
                    "align.crypto.secret debe decodificar a 16, 24 o 32 bytes "
                            + "(AES-128/192/256). Recibidos: " + keyBytes.length);
        }

        this.masterKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            // Falla de configuración del entorno, no del input: romper fuerte.
            throw new IllegalStateException("No se pudo cifrar la API key.", e);
        }
    }

    /**
     * Devuelve {@code Optional.empty()} en vez de lanzar cuando el descifrado
     * falla. No es tragarse el error: es que "no se puede descifrar" tiene una
     * causa esperable (rotaron align.crypto.secret y las keys viejas quedaron
     * ilegibles) y el servicio la trata como "no hay key configurada", que
     * manda al usuario de vuelta al wizard. Igual queda registrado en el log.
     */
    Optional<String> decrypt(String stored) {
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            if (combined.length <= IV_LENGTH_BYTES) {
                log.warn("API key almacenada demasiado corta para contener IV + ciphertext.");
                return Optional.empty();
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return Optional.of(new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("No se pudo descifrar una API key almacenada; se tratará como no "
                    + "configurada. Causa habitual: align.crypto.secret cambió.", e);
            return Optional.empty();
        }
    }
}
