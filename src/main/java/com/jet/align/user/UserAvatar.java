package com.jet.align.user;

import com.jet.align.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto de perfil de un usuario. Vive en su propia tabla (1:1) para que los bytes
 * no se carguen junto con {@link User} en cada request autenticado.
 */
@Entity
@Table(
        name = "user_avatars",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_avatars_user",
                columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class UserAvatar extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    // byte[] sin @Lob a propósito: @Lob en Postgres mapea a oid (large objects),
    // byte[] a secas mapea a bytea, que es lo que crea V19.
    @Column(nullable = false)
    private byte[] data;
}
