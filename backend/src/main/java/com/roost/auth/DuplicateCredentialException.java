package com.roost.auth;

/**
 * The chosen username or email is already taken. Registration inherently leaks
 * availability (a unique constraint must), so this is a distinct, honest error —
 * unlike login, which stays deliberately generic.
 */
public class DuplicateCredentialException extends RuntimeException {

    public DuplicateCredentialException() {
        super("username or email already in use");
    }
}
