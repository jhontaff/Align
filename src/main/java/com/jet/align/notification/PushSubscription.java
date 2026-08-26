package com.jet.align.notification;


import com.jet.align.common.model.BaseEntity;
import com.jet.align.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
@RequiredArgsConstructor
public class PushSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String endpoint;
    @Column(nullable = false)
    private String p256dh;
    @Column(nullable = false)
    private String auth;

}
