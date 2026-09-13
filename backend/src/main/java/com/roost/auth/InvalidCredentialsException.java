package com.roost.auth;

/**
 * Login failed. One exception for both "no such user" and "wrong password",
 * with a generic message, so an attacker cannot enumerate valid usernames.
 * Paired with a timing-safe lookup in the service.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("invalid credentials");
    }
}
