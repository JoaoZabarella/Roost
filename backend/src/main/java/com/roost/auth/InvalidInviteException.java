package com.roost.auth;

/**
 * The invite code is unknown, revoked, expired, exhausted, or bound to a
 * different email. Deliberately one exception with a generic message so the
 * caller cannot tell these cases apart (no probing of which codes exist).
 */
public class InvalidInviteException extends RuntimeException {

    public InvalidInviteException() {
        super("invalid or expired invite");
    }
}
