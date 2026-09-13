package com.roost.invite;

import com.roost.user.SystemRole;
import com.roost.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A registration invite: the ticket that lets someone create an account while
 * registration is closed. One model covers single-use and multi-use-with-quota
 * invites via {@code maxUses}; {@code uses} is the consumed count.
 *
 * <p>The raw code is never stored — only its SHA-256 hash ({@code codeHash}),
 * so a database leak does not hand out usable invites. The high-entropy random
 * code makes a plain hash sufficient here (unlike passwords, which need a slow,
 * salted KDF).
 *
 * <p>An optional {@code email} binds the invite to one address: when set, only a
 * registration with that exact email may redeem it, so a forwarded code cannot
 * be used by someone else. When null, the invite is open to whoever holds it.
 *
 * <p>Validity is guarded atomically at redemption time by a conditional
 * {@code UPDATE} (see {@code InviteRepository#consume}); the helpers here are
 * for read-side checks and messages, not for the race-safe decision.
 */
@Entity
@Table(name = "invites")
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Column(length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SystemRole role;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int uses;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Invite() {
        // JPA
    }

    public Invite(
            String codeHash,
            String email,
            SystemRole role,
            int maxUses,
            OffsetDateTime expiresAt,
            User createdBy) {
        if (maxUses < 1) {
            throw new IllegalArgumentException("maxUses must be >= 1");
        }
        this.codeHash = codeHash;
        this.email = email;
        this.role = role;
        this.maxUses = maxUses;
        this.uses = 0;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.createdBy = createdBy;
    }

    /** True when this invite is not bound to a specific address. */
    public boolean isOpen() {
        return email == null;
    }

    /** True when the invite is bound and the given address matches it. */
    public boolean matchesEmail(String candidate) {
        return email != null && email.equalsIgnoreCase(candidate);
    }

    public UUID getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getEmail() {
        return email;
    }

    public SystemRole getRole() {
        return role;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getUses() {
        return uses;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void revoke() {
        this.revoked = true;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Invite other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Constant per type: stable across the persist lifecycle and proxy-safe.
        return Hibernate.getClass(this).hashCode();
    }
}
