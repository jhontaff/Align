package com.jet.align.task;

import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    Optional<Task> findByIdAndUser(UUID id, User user);
    List<Task> findAllByDueDateAndStatusNot(LocalDate dueDate, TaskStatus status);
}

