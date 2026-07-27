package com.jet.align.task;

import com.jet.align.task.enums.TaskStatus;
import com.jet.align.task.dto.TaskRequest;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.dto.TaskUpdateRequest;
import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {
    TaskResponse createTask(TaskRequest request, User user);
    public TaskResponse getTaskById (UUID id, User user);
    public TaskResponse updateTask(
            UUID id,
            TaskUpdateRequest request,
            User user
    );
    public void deleteTask(UUID id, User user);
    public Page<TaskResponse> getTasks(User user, Pageable pageable, TaskStatus status);
}
