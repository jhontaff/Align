package com.jet.align.integration;

import com.jet.align.habit.HabitCompletionRepository;
import com.jet.align.habit.HabitRepository;
import com.jet.align.habit.HabitService;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
import com.jet.align.support.AbstractIntegrationTest;
import com.jet.align.user.User;
import com.jet.align.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.jet.align.support.IntegrationTestData.newUser;
import static org.assertj.core.api.Assertions.assertThat;

class HabitCascadeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    HabitService habitService;

    @Autowired
    HabitRepository habitRepository;

    @Autowired
    HabitCompletionRepository habitCompletionRepository;

    @Autowired
    UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        habitCompletionRepository.deleteAll();
        habitRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(newUser());
    }

    @Test
    void deletingAHabitAlsoRemovesItsCompletionRows() {
        HabitResponse habit = habitService.createHabit(user, new HabitRequest("Meditar", null));
        habitService.completeHabit(user, habit.id());
        assertThat(habitCompletionRepository.count()).isEqualTo(1);

        habitService.deleteHabit(user, habit.id());

        assertThat(habitRepository.findById(habit.id())).isEmpty();
        assertThat(habitCompletionRepository.count()).isZero();
    }
}
