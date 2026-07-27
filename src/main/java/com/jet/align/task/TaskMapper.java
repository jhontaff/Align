package com.jet.align.task;

import com.jet.align.task.dto.TaskRequest;
import com.jet.align.task.dto.TaskResponse;
import com.jet.align.task.dto.TaskUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    Task toEntity(TaskRequest request);

    TaskResponse toResponse(Task task);

    void updateEntity(TaskUpdateRequest request, @MappingTarget Task task);

}
