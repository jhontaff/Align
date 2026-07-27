package com.jet.align.user;

import com.jet.align.auth.dto.RegisterRequest;
import com.jet.align.user.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequest request);

    UserResponse toResponse(User user);

}