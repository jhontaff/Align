package com.jet.align.task;

import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.task.enums.TaskStatus;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;
    private final TaskMapper mapper;

    public TaskServiceImpl(TaskRepository repository, TaskMapper mapper) {
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

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(User user, Pageable pageable) {

        return repository.findAllByUser(user, pageable)
                .map(mapper::toResponse);
    }

}