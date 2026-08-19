package com.jet.align.habit.impl;

import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.user.User;
import com.jet.align.habit.dto.HabitRequest;

import java.util.List;
import java.util.UUID;

public class HabitServiceImpl implements HabitService {

    @Override
    public HabitResponse createHabit(User user, HabitRequest habitRequest) {
        return null;
    }

    @Override
    public List<HabitResponse> getHabits(User user) {
        return List.of();
    }

    @Override
    public HabitResponse updateHabit(User user, UUID habitId, HabitRequest habitRequest) {
        return null;
    }

    @Override
    public void deleteHabit(User user, UUID habitId) {

    }

    @Override
    public HabitResponse completeHabit(User user, UUID habitId) {
        return null;
    }

}
