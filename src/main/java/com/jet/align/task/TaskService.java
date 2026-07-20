package com.jet.align.task;

import com.jet.align.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {

    public TaskResponse createTask(TaskRequest request, User user);

    public TaskResponse getTaskById (UUID id, User user);

    public Page<TaskResponse> getTasks(User user, Pageable pageable);
}
