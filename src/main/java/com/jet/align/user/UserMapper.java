package com.jet.align.user;

import com.jet.align.auth.dto.RegisterRequest;
import com.jet.align.user.dto.ProfileUpdateRequest;
import com.jet.align.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequest request);

    // hasAvatar no es una columna de User: lo calcula el service y lo pasa aparte,
    // igual que HabitMapper con los streaks.
    @Mapping(target = "hasAvatar", source = "hasAvatar")
    UserResponse toResponse(User user, boolean hasAvatar);

    void updateEntity(ProfileUpdateRequest request, @MappingTarget User user);

}
