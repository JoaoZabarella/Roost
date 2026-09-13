package com.roost.auth;

/**
 * The active {@link RegistrationPolicy} rejected the attempt — e.g. invite-only
 * mode with no invite supplied.
 */
public class RegistrationForbiddenException extends RuntimeException {

    public RegistrationForbiddenException(String message) {
        super(message);
    }
}
