package com.jet.align.habit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.jet.align.habit.dto.HabitRequest;
import com.jet.align.habit.dto.HabitResponse;
@Mapper(componentModel = "spring")
public interface HabitMapper {

    Habit toEntity(HabitRequest request);

    @Mapping(target = "currentStreak", source = "currentStreak")
    @Mapping(target = "longestStreak", source = "longestStreak")
    @Mapping(target = "completedToday", source = "completedToday")
    HabitResponse toResponse(Habit habit, int currentStreak, int longestStreak, boolean completedToday);

    void updateEntity(HabitRequest request, @MappingTarget Habit habit);
}

