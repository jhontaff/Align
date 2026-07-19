package com.jet.align.common.model;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {


    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Setter(AccessLevel.NONE)
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private Instant updatedAt;
}
