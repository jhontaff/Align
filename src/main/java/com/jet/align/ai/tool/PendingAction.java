package com.jet.align.ai.tool;

import com.jet.align.common.model.BaseEntity;
import com.jet.align.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pending_actions")
@Getter
@Setter
@RequiredArgsConstructor
public class PendingAction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String toolName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String argumentsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PendingActionStatus status;
}
