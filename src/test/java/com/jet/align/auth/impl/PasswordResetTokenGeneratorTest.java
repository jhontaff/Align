package com.jet.align.auth.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenGeneratorTest {

    private final PasswordResetTokenGenerator generator = new PasswordResetTokenGenerator();

    @Test
    void generateRawToken_devuelve_valores_distintos_en_cada_llamada() {
        assertThat(generator.generateRawToken()).isNotEqualTo(generator.generateRawToken());
    }

    @Test
    void generateRawToken_es_url_safe_para_ir_en_el_query_string_del_link() {
        String token = generator.generateRawToken();

        // 32 bytes en base64url sin padding = 43 chars, solo [A-Za-z0-9_-].
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hash_es_determinista_para_poder_buscar_por_igualdad() {
        // Si esto fallara, findByTokenHash nunca encontraría nada: es la razón
        // por la que no se usa bcrypt acá.
        assertThat(generator.hash("abc")).isEqualTo(generator.hash("abc"));
    }

    @Test
    void hash_devuelve_sha256_en_hex() {
        assertThat(generator.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hash_de_tokens_distintos_no_coincide() {
        assertThat(generator.hash("abc")).isNotEqualTo(generator.hash("abd"));
    }
}
