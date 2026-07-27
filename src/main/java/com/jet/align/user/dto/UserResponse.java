package com.jet.align.user.dto;

import com.jet.align.user.Role;

import java.util.UUID;

public record UserResponse(

        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role

) {}