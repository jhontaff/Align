package com.jet.align.task.model;

import com.jet.align.task.Task;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    Task toEntity(TaskRequest request);

    TaskResponse toResponse(Task task);

    void updateEntity(
            TaskUpdateRequest request,
            @MappingTarget Task task
    );

}
