package com.jet.align.ai.tool;

import com.jet.align.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingActionRepository extends JpaRepository<PendingAction, UUID> {

    Optional<PendingAction> findByIdAndUser(UUID id, User user);
    List<PendingAction> findByStatusAndCreatedAtBefore(PendingActionStatus status, Instant cutoff);

}
