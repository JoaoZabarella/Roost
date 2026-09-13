package com.roost.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void mapsDataIntegrityViolationToConflict() {
        // A unique-constraint race in register must surface as 409, not 500.
        ProblemDetail problem = handler.onDataIntegrity(
            new DataIntegrityViolationException("uq_users_username"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        // Generic, leak-free message.
        assertThat(problem.getDetail()).isEqualTo("username or email already in use");
    }
}
