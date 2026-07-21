package com.jet.align.task;

import com.jet.align.task.model.TaskRequest;
import com.jet.align.task.model.TaskResponse;
import com.jet.align.task.model.TaskUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequest request) {

        Task task = new Task();

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());

        return task;
    }

    public TaskResponse toResponse(Task task) {

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
    public void updateEntity(TaskUpdateRequest request, Task task) {

        if (request == null) {
            return;
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
    }
}
