package com.jet.align.habit;

import com.jet.align.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitRepository extends JpaRepository<Habit, UUID> {

    List<Habit> findByUserOrderByCreatedAtDesc(User user);
    Optional<Habit> findByIdAndUser(UUID id, User user);

}
