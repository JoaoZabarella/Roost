package com.roost.invite;

import static org.assertj.core.api.Assertions.assertThat;

import com.roost.TestcontainersConfiguration;
import com.roost.user.SystemRole;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Proves the atomic invite consume against real Postgres: the conditional
 * {@code UPDATE} enforces quota, expiry, and revocation as an all-in-one guard.
 * This is the race-safe heart of redemption, so it is tested against the DB, not
 * with mocks.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class InviteRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private InviteRepository invites;

    private static Invite invite(int maxUses, OffsetDateTime expiresAt) {
        return new Invite("hash-" + maxUses + "-" + expiresAt, null,
            SystemRole.MEMBER, maxUses, expiresAt, null);
    }

    @Test
    void consumeClaimsSingleUseThenRejects() {
        Invite invite = invites.save(invite(1, null));
        em.flush();

        assertThat(invites.consume(invite.getId(), OffsetDateTime.now())).isEqualTo(1);
        // Exhausted: the second claim finds uses == max_uses and updates nothing.
        assertThat(invites.consume(invite.getId(), OffsetDateTime.now())).isEqualTo(0);
    }

    @Test
    void consumeHonoursMultiUseQuota() {
        Invite invite = invites.save(invite(3, null));
        em.flush();
        OffsetDateTime now = OffsetDateTime.now();

        assertThat(invites.consume(invite.getId(), now)).isEqualTo(1);
        assertThat(invites.consume(invite.getId(), now)).isEqualTo(1);
        assertThat(invites.consume(invite.getId(), now)).isEqualTo(1);
        assertThat(invites.consume(invite.getId(), now)).isEqualTo(0); // quota spent

        em.clear();
        assertThat(em.find(Invite.class, invite.getId()).getUses()).isEqualTo(3);
    }

    @Test
    void consumeRejectsExpiredInvite() {
        Invite invite = invites.save(invite(1, OffsetDateTime.now().minusMinutes(1)));
        em.flush();

        assertThat(invites.consume(invite.getId(), OffsetDateTime.now())).isEqualTo(0);
    }

    @Test
    void consumeRejectsRevokedInvite() {
        Invite invite = invite(1, null);
        invite.revoke();
        invites.save(invite);
        em.flush();

        assertThat(invites.consume(invite.getId(), OffsetDateTime.now())).isEqualTo(0);
    }

    @Test
    void findsAndDetectsByCodeHash() {
        invites.save(invite(1, null));
        em.flush();
        String codeHash = "hash-1-null";

        assertThat(invites.findByCodeHash(codeHash)).isPresent();
        assertThat(invites.existsByCodeHash(codeHash)).isTrue();
        assertThat(invites.existsByCodeHash("nope")).isFalse();
    }
}
