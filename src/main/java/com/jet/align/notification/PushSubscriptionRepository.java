package com.jet.align.notification;

import com.jet.align.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    Optional<PushSubscription> findByEndpoint(String endpoint);
    void deleteByUserAndEndpoint(User user, String endpoint);
    List<PushSubscription> findByUser(User user);
}
