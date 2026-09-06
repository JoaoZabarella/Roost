package com.roost.invite;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

    Optional<Invite> findByCodeHash(String codeHash);

    boolean existsByCodeHash(String codeHash);

    /**
     * Atomically claims one use of an invite. The {@code WHERE} clause is the
     * guard: it increments {@code uses} only if the invite is still valid
     * (not revoked, quota left, not expired). Returns the number of rows
     * updated — {@code 1} means the caller won a use, {@code 0} means the
     * invite was invalid or exhausted.
     *
     * <p>Doing the check and the increment in one statement makes it race-safe:
     * concurrent redemptions serialize on the row lock, so a {@code maxUses=1}
     * invite can never be claimed twice.
     */
    @Modifying
    @Query("""
        update Invite i
           set i.uses = i.uses + 1
         where i.id = :id
           and i.revoked = false
           and i.uses < i.maxUses
           and (i.expiresAt is null or i.expiresAt > :now)
        """)
    int consume(@Param("id") UUID id, @Param("now") OffsetDateTime now);
}
