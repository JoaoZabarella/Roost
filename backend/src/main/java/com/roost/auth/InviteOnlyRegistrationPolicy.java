package com.roost.auth;

import com.roost.auth.dto.RegisterRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Closed registration: every attempt must carry an invite code. The code's
 * validity is checked later, when the service redeems it; this policy only
 * enforces that one was supplied at all.
 */
@Component
public class InviteOnlyRegistrationPolicy implements RegistrationPolicy {

    @Override
    public void enforce(RegisterRequest request) {
        if (!StringUtils.hasText(request.inviteCode())) {
            throw new RegistrationForbiddenException("registration requires an invite");
        }
    }
}
