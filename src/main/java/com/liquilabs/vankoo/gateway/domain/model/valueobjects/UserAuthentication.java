package com.liquilabs.vankoo.gateway.domain.model.valueobjects;

import java.util.List;

public record UserAuthentication(String userId, String email, List<String> roles) {
    public UserAuthentication {
        if (userId == null || email == null) {
            throw new IllegalArgumentException("UserId and Email cannot be null");
        }
    }
}