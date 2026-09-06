package com.roost.auth.dto;

import com.roost.user.SystemRole;
import com.roost.user.User;
import java.util.UUID;

/** Minimal view of a freshly registered account (no secrets). */
public record RegisteredUserResponse(
    UUID id, String username, String displayName, SystemRole role) {

    public static RegisteredUserResponse from(User user) {
        return new RegisteredUserResponse(
            user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
