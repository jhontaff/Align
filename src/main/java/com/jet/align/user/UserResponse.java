package com.jet.align.user;

import java.util.UUID;

public record UserResponse(

        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role

) {}