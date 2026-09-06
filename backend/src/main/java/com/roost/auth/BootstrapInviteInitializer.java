package com.roost.auth;

import com.roost.config.RoostSecurityProperties;
import com.roost.invite.Invite;
import com.roost.invite.InviteRepository;
import com.roost.user.SystemRole;
import com.roost.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Solves the bootstrap chicken-and-egg: registration needs an invite, but the
 * first invite has no admin to create it. On startup, when the users table is
 * empty, this seeds a single-use ADMIN invite from {@code BOOTSTRAP_INVITE_CODE}
 * so the very first account can be created.
 *
 * <p>Idempotent and self-limiting: it does nothing once any user exists, so the
 * env var becomes irrelevant after the first registration. To avoid a
 * dead-on-arrival deployment (up, but nobody can get in), it <em>fails fast</em>
 * when there are no users and no code was supplied.
 */
@Component
public class BootstrapInviteInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapInviteInitializer.class);

    private final UserRepository users;
    private final InviteRepository invites;
    private final RoostSecurityProperties properties;

    public BootstrapInviteInitializer(
            UserRepository users,
            InviteRepository invites,
            RoostSecurityProperties properties) {
        this.users = users;
        this.invites = invites;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }

        String code = properties.bootstrapInviteCode();
        if (!StringUtils.hasText(code)) {
            throw new IllegalStateException(
                "No users exist and BOOTSTRAP_INVITE_CODE is not set: the app would "
                    + "start with no way to create the first account. Set the env var.");
        }

        String codeHash = InviteCodes.hash(code);
        if (invites.existsByCodeHash(codeHash)) {
            return; // seeded on a previous boot, first registration not done yet
        }

        invites.save(new Invite(codeHash, null, SystemRole.ADMIN, 1, null, null));
        log.info("Seeded bootstrap ADMIN invite (single-use). It is now consumable.");
    }
}
