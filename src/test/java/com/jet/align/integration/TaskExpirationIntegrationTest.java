package com.jet.align.integration;

import com.jet.align.support.AbstractIntegrationTest;
import com.jet.align.task.Task;
import com.jet.align.task.TaskRepository;
import com.jet.align.task.TaskService;
import com.jet.align.task.enums.Priority;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import com.jet.align.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static com.jet.align.support.IntegrationTestData.newUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TaskExpirationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TaskService taskService;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(newUser());
    }

    @Test
    void expiresOverdueOpenTasksAndLeavesEverythingElseUntouched() {
        Task overduePending = persist(TaskStatus.PENDING, LocalDate.now().minusDays(1), null);
        Task overdueInProgress = persist(TaskStatus.IN_PROGRESS, LocalDate.now().minusDays(3), null);
        Task dueTomorrow = persist(TaskStatus.PENDING, LocalDate.now().plusDays(1), null);
        Task overdueButCompleted = persist(TaskStatus.COMPLETED, LocalDate.now().minusDays(5), null);

        taskService.expireOverdueTasks();

        assertThat(statusOf(overduePending)).isEqualTo(TaskStatus.EXPIRED);
        assertThat(statusOf(overdueInProgress)).isEqualTo(TaskStatus.EXPIRED);
        assertThat(statusOf(dueTomorrow)).isEqualTo(TaskStatus.PENDING);
        assertThat(statusOf(overdueButCompleted)).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void forTasksDueTodayOnlyThoseWithAPastDueTimeExpire() {
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        assumeTrue(now.isAfter(LocalTime.of(0, 5)) && now.isBefore(LocalTime.of(23, 55)),
                "skipped within 5 min of midnight UTC to keep the due-time boundary deterministic");

        Task pastTime = persist(TaskStatus.PENDING, LocalDate.now(), now.minusMinutes(5));
        Task futureTime = persist(TaskStatus.PENDING, LocalDate.now(), now.plusMinutes(5));
        Task noTime = persist(TaskStatus.PENDING, LocalDate.now(), null);

        taskService.expireOverdueTasks();

        assertThat(statusOf(pastTime)).isEqualTo(TaskStatus.EXPIRED);
        assertThat(statusOf(futureTime)).isEqualTo(TaskStatus.PENDING);
        assertThat(statusOf(noTime)).isEqualTo(TaskStatus.PENDING);
    }

    private Task persist(TaskStatus status, LocalDate dueDate, LocalTime dueTime) {
        Task task = new Task();
        task.setTitle("t");
        task.setDescription("d");
        task.setPriority(Priority.MEDIUM);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setDueTime(dueTime);
        task.setUser(user);
        return taskRepository.save(task);
    }

    private TaskStatus statusOf(Task task) {
        return taskRepository.findById(task.getId()).orElseThrow().getStatus();
    }
}
