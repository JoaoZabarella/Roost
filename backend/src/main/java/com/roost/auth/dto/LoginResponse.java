package com.roost.auth.dto;

import java.time.Instant;

/**
 * Issued access token. {@code tokenType} is always {@code Bearer}; the client
 * sends it as {@code Authorization: Bearer <accessToken>}.
 */
public record LoginResponse(String tokenType, String accessToken, Instant expiresAt) {

    public static LoginResponse bearer(String accessToken, Instant expiresAt) {
        return new LoginResponse("Bearer", accessToken, expiresAt);
    }
}
