package com.jet.align.habit;

import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.user.User;
import java.util.List;
import java.util.UUID;

public interface HabitService {
    HabitResponse createHabit(User user, HabitRequest habitRequest);
    List<HabitResponse> getHabits(User user);
    HabitResponse updateHabit(User user, UUID habitId, HabitRequest habitRequest);
    void deleteHabit(User user, UUID habitId);
    HabitResponse completeHabit(User user, UUID habitId);
    HabitResponse getHabitById(User user, UUID habitId);
}
