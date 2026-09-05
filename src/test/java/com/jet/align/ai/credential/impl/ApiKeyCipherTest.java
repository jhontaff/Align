package com.jet.align.ai.credential.impl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyCipherTest {

    private static final String API_KEY = "AIzaSyEstoEsUnaApiKeyDePrueba";

    @Test
    void cifra_y_vuelve_a_descifrar_la_misma_key() {
        ApiKeyCipher cipher = new ApiKeyCipher(secretOf((byte) 1));

        String encrypted = cipher.encrypt(API_KEY);

        assertThat(encrypted).doesNotContain(API_KEY);
        assertThat(cipher.decrypt(encrypted)).contains(API_KEY);
    }

    /**
     * El IV se genera al azar en cada operación. Si dos cifrados del mismo
     * texto dieran idéntico, sería señal de que el IV quedó fijo, que es
     * exactamente lo que rompe la seguridad de AES-GCM.
     */
    @Test
    void dos_cifrados_de_la_misma_key_dan_resultados_distintos() {
        ApiKeyCipher cipher = new ApiKeyCipher(secretOf((byte) 1));

        assertThat(cipher.encrypt(API_KEY)).isNotEqualTo(cipher.encrypt(API_KEY));
    }

    /**
     * Simula que rotaron align.crypto.secret: lo guardado queda ilegible, y el
     * servicio lo trata como "no hay key configurada".
     */
    @Test
    void no_descifra_si_la_master_key_cambio() {
        String encrypted = new ApiKeyCipher(secretOf((byte) 1)).encrypt(API_KEY);

        assertThat(new ApiKeyCipher(secretOf((byte) 2)).decrypt(encrypted)).isEmpty();
    }

    @Test
    void devuelve_vacio_si_el_contenido_almacenado_esta_corrupto() {
        ApiKeyCipher cipher = new ApiKeyCipher(secretOf((byte) 1));

        assertThat(cipher.decrypt("no-es-base64-valido-!!!")).isEmpty();
        assertThat(cipher.decrypt(Base64.getEncoder().encodeToString(new byte[4]))).isEmpty();
    }

    @Test
    void rechaza_una_master_key_de_largo_invalido() {
        assertThatThrownBy(() -> new ApiKeyCipher(
                Base64.getEncoder().encodeToString(new byte[10])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("16, 24 o 32 bytes");
    }

    @Test
    void rechaza_una_master_key_que_no_es_base64() {
        assertThatThrownBy(() -> new ApiKeyCipher("esto no es base64 !!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    private static String secretOf(byte fill) {
        byte[] key = new byte[32];
        Arrays.fill(key, fill);
        return Base64.getEncoder().encodeToString(key);
    }
}
