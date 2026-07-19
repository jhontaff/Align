package com.jet.align.user;

import com.jet.align.auth.dto.RegisterRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequest request);

}