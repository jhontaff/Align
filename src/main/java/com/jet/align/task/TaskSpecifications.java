package com.jet.align.task;

import com.jet.align.task.dto.TaskFilter;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TaskSpecifications {

    public static Specification<Task> withFilter(User user, TaskFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user"), user));
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.dueFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filter.dueFrom()));
            }
            if (filter.dueTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filter.dueTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Task> expirable(List<TaskStatus> eligibleStatuses, LocalDate today, LocalTime now) {
        return (root, query, cb) -> {
            Predicate overdue = cb.lessThan(root.get("dueDate"), today);
            Predicate dueTodayPastTime = cb.and(
                    cb.equal(root.get("dueDate"), today),
                    cb.isNotNull(root.get("dueTime")),
                    cb.lessThan(root.get("dueTime"), now)
            );
            return cb.and(
                    root.get("status").in(eligibleStatuses),
                    cb.or(overdue, dueTodayPastTime)
            );
        };
    }

    public static Specification<Task> dueForReminder(List<TaskStatus> eligibleStatuses,
                                                     LocalDate today, LocalTime now) {
        return (root, query, cb) -> cb.and(
                root.get("status").in(eligibleStatuses),
                cb.equal(root.get("dueDate"), today),
                cb.isNotNull(root.get("dueTime")),
                cb.lessThanOrEqualTo(root.get("dueTime"), now),
                cb.isFalse(root.get("reminderSent"))
        );
    }

    private TaskSpecifications() {}
}
