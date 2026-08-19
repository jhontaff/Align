package com.jet.align.habit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, UUID> {

    boolean existsByHabitAndDate(Habit habit, LocalDate date);
    List<HabitCompletion> findByHabitOrderByDateDesc(Habit habit);

}
