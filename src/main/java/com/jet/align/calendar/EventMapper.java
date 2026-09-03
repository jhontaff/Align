package com.jet.align.calendar;

import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EventMapper {

    Event toEntity(EventRequest request);

    EventResponse toResponse(Event event);

    @Mapping(target = "reminderAt", ignore = true)
    @Mapping(target = "reminderSent", ignore = true)
    void updateEntity(EventRequest request, @MappingTarget Event event);
}

