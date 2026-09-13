package com.roost.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates auth failures into RFC 7807 {@link ProblemDetail} responses. The
 * messages are intentionally coarse: login and invite errors say as little as
 * possible so they cannot be used to probe which usernames or codes exist.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(RegistrationForbiddenException.class)
    ProblemDetail onForbidden(RegistrationForbiddenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(InvalidInviteException.class)
    ProblemDetail onInvalidInvite(InvalidInviteException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DuplicateCredentialException.class)
    ProblemDetail onDuplicate(DuplicateCredentialException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * The service pre-checks username/email availability, but a concurrent
     * registration can still trip a unique constraint between that check and the
     * insert. The DB is the authority: translate its violation to the same 409 a
     * duplicate gets, so a race never surfaces as a 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onDataIntegrity(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "username or email already in use");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail onBadCredentials(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
}
