package com.jet.align.ai.memory;


import com.jet.align.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, UUID> {
    Optional<ConversationHistory> findByUser(User user);

}
