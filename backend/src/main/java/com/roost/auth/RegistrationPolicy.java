package com.roost.auth;

import com.roost.auth.dto.RegisterRequest;

/**
 * Decides whether a registration attempt is allowed at all, independent of the
 * request's field validity. This is the Strategy seam that lets registration
 * modes vary without touching {@link AuthService}: invite-only today, an open
 * (public) policy with anti-abuse checks in a later PR.
 */
public interface RegistrationPolicy {

    /**
     * Allows the attempt, or throws {@link RegistrationForbiddenException} if the
     * current mode forbids it.
     */
    void enforce(RegisterRequest request);
}
