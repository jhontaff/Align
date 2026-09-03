package com.jet.align.calendar;

import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findByIdAndUser(UUID id, User user);

    Page<Event> findByUserOrderByStartAtAsc(User user, Pageable pageable);

    Page<Event> findByUserAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
            User user, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<Event> findByReminderSentFalseAndReminderAtLessThanEqual(LocalDateTime cutoff);
}

