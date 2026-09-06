package com.roost.invite;

import com.roost.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Audit row recording that a {@link User} redeemed an {@link Invite}. Kept
 * separate from the invite's {@code uses} counter so a multi-use invite leaves
 * a trail of exactly who joined through it. The unique {@code (invite_id,
 * user_id)} pair keeps one user from being recorded twice against the same
 * invite.
 */
@Entity
@Table(
    name = "invite_redemptions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_invite_redemptions_invite_user",
        columnNames = {"invite_id", "user_id"}))
public class InviteRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_id", nullable = false, updatable = false)
    private Invite invite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private OffsetDateTime redeemedAt;

    protected InviteRedemption() {
        // JPA
    }

    public InviteRedemption(Invite invite, User user) {
        this.invite = invite;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public Invite getInvite() {
        return invite;
    }

    public User getUser() {
        return user;
    }

    public OffsetDateTime getRedeemedAt() {
        return redeemedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InviteRedemption other)) {
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
