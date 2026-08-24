package com.jet.align.ai.memory;

import com.jet.align.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, UUID> {

    List<UserMemory> findByUserOrderByCreatedAtDesc(User user);
    Optional<UserMemory> findByIdAndUser(UUID id, User user);
}
