package com.jet.align.support;

import com.jet.align.user.Role;
import com.jet.align.user.User;

import java.util.UUID;

public final class IntegrationTestData {

    private IntegrationTestData() {}

    public static User newUser() {
        return User.builder()
                .email("user-" + UUID.randomUUID() + "@test.local")
                .password("irrelevant-hash")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .enabled(true)
                .build();
    }
}
