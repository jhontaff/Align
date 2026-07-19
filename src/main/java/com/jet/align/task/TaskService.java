package com.jet.align.task;

import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.task.enums.TaskStatus;
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
    public TaskResponse createTask(TaskRequest taskRequest) {
        Task task = mapper.toEntity(taskRequest);
        task.setStatus(TaskStatus.PENDING);
        Task savedTask = repository.save(task);
        return mapper.toResponse(savedTask);
    }
    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id) {
        Task task = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return mapper.toResponse(task);
    }

}
