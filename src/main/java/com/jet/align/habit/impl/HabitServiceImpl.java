package com.jet.align.habit.impl;

import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.habit.*;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HabitServiceImpl implements HabitService {

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository habitCompletionRepository;
    private final HabitMapper mapper;
    private final ZoneId timezone;

    private static final String HABIT_NOT_FOUND_MESSAGE = "Habit not found with id: ";

    public HabitServiceImpl(HabitRepository habitRepository,
                            HabitMapper mapper,
                            HabitCompletionRepository habitCompletionRepository,
                            @Value("${align.timezone}") String timezone) {
        this.habitRepository = habitRepository;
        this.mapper = mapper;
        this.habitCompletionRepository = habitCompletionRepository;
        this.timezone = ZoneId.of(timezone);
    }

    @Override
    @Transactional
    public HabitResponse createHabit(User user, HabitRequest habitRequest) {
        Habit habit  = mapper.toEntity(habitRequest);
        habit.setUser(user);
        return mapper.toResponse(habitRepository.save(habit), 0, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitResponse> getHabits(User user) {
        List<Habit> habits = habitRepository.findByUserOrderByCreatedAtDesc(user);
        return habits.stream()
                .map(habit -> {
                    List<HabitCompletion> completions = habitCompletionRepository.findByHabitOrderByDateDesc(habit);
                    int currentStreak = calculateStreak(completions);
                    int longestStreak = calculateLongestStreak(completions);
                    return mapper.toResponse(habit, currentStreak, longestStreak);
                })
                .toList();
    }

    @Override
    @Transactional
    public HabitResponse updateHabit(User user, UUID habitId, HabitRequest habitRequest) {
        Habit habit = habitRepository.findByIdAndUser(habitId, user)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_NOT_FOUND_MESSAGE + habitId));
        mapper.updateEntity(habitRequest, habit);

        List<HabitCompletion> completions = habitCompletionRepository.findByHabitOrderByDateDesc(habit);
        int currentStreak = calculateStreak(completions);
        int longestStreak = calculateLongestStreak(completions);
        return mapper.toResponse(habit, currentStreak, longestStreak);

    }

    @Override
    @Transactional(readOnly = true)
    public HabitResponse getHabitById(User user, UUID habitId) {
        Habit habit = habitRepository.findByIdAndUser(habitId, user)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_NOT_FOUND_MESSAGE + habitId));

        List<HabitCompletion> completions = habitCompletionRepository.findByHabitOrderByDateDesc(habit);
        int currentStreak = calculateStreak(completions);
        int longestStreak = calculateLongestStreak(completions);
        return mapper.toResponse(habit, currentStreak, longestStreak);

    }


    @Override
    @Transactional
    public void deleteHabit(User user, UUID habitId) {
        Habit habit = habitRepository.findByIdAndUser(habitId, user)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_NOT_FOUND_MESSAGE + habitId));
        habitRepository.delete(habit);
    }

    @Override
    @Transactional
    public HabitResponse completeHabit(User user, UUID habitId) {
        Habit habit = habitRepository.findByIdAndUser(habitId, user)
                .orElseThrow(() -> new ResourceNotFoundException(HABIT_NOT_FOUND_MESSAGE + habitId));

        LocalDate today = LocalDate.now(timezone);
        if (!habitCompletionRepository.existsByHabitAndDate(habit, today)) {
            HabitCompletion completion = new HabitCompletion();
            completion.setHabit(habit);
            completion.setDate(today);
            habitCompletionRepository.save(completion);
        }

        List<HabitCompletion> completions = habitCompletionRepository.findByHabitOrderByDateDesc(habit);
        int currentStreak = calculateStreak(completions);
        int longestStreak = calculateLongestStreak(completions);

        return mapper.toResponse(habit, currentStreak, longestStreak);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Habit> findHabitsAtRisk() {
        LocalDate today = LocalDate.now(timezone);
        return habitRepository.findAll().stream()
                .filter(habit -> {
                    List<HabitCompletion> completions = habitCompletionRepository.findByHabitOrderByDateDesc(habit);
                    boolean completedToday = !completions.isEmpty() && completions.getFirst().getDate().equals(today);
                    return !completedToday && calculateStreak(completions) > 0;
                })
                .toList();
    }

    private int calculateStreak(List<HabitCompletion> completions) {
        if (completions.isEmpty()) return 0;

        LocalDate today = LocalDate.now(timezone);
        LocalDate mostRecent = completions.getFirst().getDate();
        if (mostRecent.isBefore(today.minusDays(1))) return 0;

        int streak = 1;
        LocalDate expected = mostRecent.minusDays(1);
        for (int i = 1; i < completions.size(); i++) {
            if (!completions.get(i).getDate().equals(expected)) break;
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }

    private int calculateLongestStreak(List<HabitCompletion> completions) {
        if (completions.isEmpty()) return 0;
        int longest = 1;
        int current = 1;
        for (int i = 1; i < completions.size(); i++) {
            LocalDate previous = completions.get(i - 1).getDate();
            if (completions.get(i).getDate().equals(previous.minusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            longest = Math.max(longest, current);
        }
        return longest;
    }

}
