package com.jet.align.ai.credential;

import com.jet.align.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LlmCredentialRepository extends JpaRepository<LlmCredential, UUID> {

    Optional<LlmCredential> findByUser(User user);
}
