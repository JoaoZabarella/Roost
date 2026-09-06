package com.roost.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Field-shape validation lives here; whether an invite is
 * <em>required</em> is a {@code RegistrationPolicy} decision, so {@code
 * inviteCode} is intentionally not {@code @NotBlank} — an open-registration mode
 * could accept a blank one.
 */
public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 32) String username,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(min = 1, max = 64) String displayName,
    String inviteCode) {
}
