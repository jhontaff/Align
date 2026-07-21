package com.jet.align.task;

import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.task.model.TaskRequest;
import com.jet.align.task.model.TaskResponse;
import com.jet.align.task.model.TaskUpdateRequest;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository repository;
    private final TaskMapper mapper;

    public TaskService(TaskRepository repository, TaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public TaskResponse createTask(
            TaskRequest request,
            User user
    ) {

        Task task = mapper.toEntity(request);
        task.setStatus(TaskStatus.PENDING);
        task.setUser(user);
        return mapper.toResponse(repository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById (UUID id, User user) {
        Task task = repository.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return mapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(User user, Pageable pageable, TaskStatus status) {

        Page<Task> tasks;

        if (status == null) {
            tasks = repository.findAllByUser(user, pageable);
        } else {
            tasks = repository.findAllByUserAndStatus(user, status, pageable);
        }

        return tasks.map(mapper::toResponse);
    }

    @Transactional
    public TaskResponse updateTask(
            UUID id,
            TaskUpdateRequest request,
            User user
    ) {
        Task task = repository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + id));

        mapper.updateEntity(request, task);

        Task updatedTask = repository.save(task);

        return mapper.toResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(UUID id, User user) {
        Task task = repository.findByIdAndUser(id, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found with id: " + id));

        repository.delete(task);
    }

}