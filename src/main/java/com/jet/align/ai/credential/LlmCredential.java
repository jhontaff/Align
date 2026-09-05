package com.jet.align.ai.credential;

import com.jet.align.common.model.BaseEntity;
import com.jet.align.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API key del proveedor de LLM que pertenece a un usuario (BYOK).
 *
 * <p>{@code encryptedKey} nunca contiene la key en claro: guarda el resultado
 * de {@code ApiKeyCipher.encrypt}. {@code lastFour} existe solo para que el
 * usuario reconozca cuál key tiene configurada sin que el backend tenga que
 * devolvérsela.
 */
@Entity
@Table(
        name = "llm_credentials",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_llm_credentials_user",
                columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class LlmCredential extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedKey;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;
}
