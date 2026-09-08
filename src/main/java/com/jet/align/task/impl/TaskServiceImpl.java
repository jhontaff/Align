package com.jet.align.task.impl;

import com.jet.align.common.exception.BusinessException;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.task.*;
import com.jet.align.task.dto.TaskFilter;
import com.jet.align.task.dto.TaskRequest;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.dto.TaskUpdateRequest;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {
    private static final String TASK_NOT_FOUND_MESSAGE = "Task not found with id: ";
    
    private final TaskRepository repository;
    private final TaskMapper mapper;
    private final ZoneId timezone;

    public TaskServiceImpl(TaskRepository repository, TaskMapper mapper,
                           @Value("${align.timezone}") String timezone) {
        this.repository = repository;
        this.mapper = mapper;
        this.timezone = ZoneId.of(timezone);
    }
    @Override
    @Transactional
    public TaskResponse createTask(TaskRequest request, User user) {
        requireDueDate(request.dueDate());
        Task task = mapper.toEntity(request);
        task.setStatus(TaskStatus.PENDING);
        task.setUser(user);
        return mapper.toResponse(repository.save(task));
    }


    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById (UUID id, User user) {
        Task task = repository.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND_MESSAGE + id));
        return mapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(User user, Pageable pageable, TaskFilter filter) {
        return repository.findAll(TaskSpecifications.withFilter(user, filter), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID id, TaskUpdateRequest request, User user) {
        requireDueDate(request.dueDate());
        Task task = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND_MESSAGE + id));


        LocalDate previousDueDate = task.getDueDate();
        LocalTime previousDueTime = task.getDueTime();

        mapper.updateEntity(request, task);
        reArmReminderIfDueChanged(task, previousDueDate, previousDueTime);

        return mapper.toResponse(repository.save(task));
    }

    private void reArmReminderIfDueChanged(Task task, LocalDate previousDueDate, LocalTime previousDueTime) {
        boolean dueChanged = !Objects.equals(previousDueDate, task.getDueDate())
                || !Objects.equals(previousDueTime, task.getDueTime());
        if (dueChanged) {
            task.setReminderSent(false);
        }
    }


    @Override
    @Transactional
    public void deleteTask(UUID id, User user) {
        Task task = repository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                TASK_NOT_FOUND_MESSAGE + id));

        repository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findTasksDueToday() {
        return repository.findAllByDueDateAndDueTimeIsNullAndStatusNot(
                LocalDate.now(timezone), TaskStatus.COMPLETED);
    }


    @Override
    @Transactional
    public void expireOverdueTasks() {
        List<Task> tasks = repository.findAll(TaskSpecifications.expirable(
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS),
                LocalDate.now(timezone),
                LocalTime.now(timezone)));
        tasks.forEach(t -> t.setStatus(TaskStatus.EXPIRED));
        repository.saveAll(tasks);
    }


    @Override
    @Transactional
    public void setTaskStatus(UUID id, User user, TaskStatus status) {
        Task task = repository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                TASK_NOT_FOUND_MESSAGE + id));
        task.setStatus(status);
        repository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findTasksDueForReminder() {
        return repository.findAll(TaskSpecifications.dueForReminder(
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS),
                LocalDate.now(timezone),
                LocalTime.now(timezone)));
    }

    @Override
    @Transactional
    public void markReminderSent(UUID taskId) {
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND_MESSAGE + taskId));
        task.setReminderSent(true);
        repository.save(task);
    }

    private void requireDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new BusinessException("Due date is required.");
        }
    }


}