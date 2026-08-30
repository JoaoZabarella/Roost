package com.roost.server;

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
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Join row placing a {@link User} in a {@link Server} with a role. A surrogate
 * UUID id plus a unique {@code (server_id, user_id)} constraint keeps a user
 * from joining the same server twice while staying easy to reference.
 */
@Entity
@Table(
    name = "server_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_server_members_server_user",
        columnNames = {"server_id", "user_id"}))
public class ServerMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false, updatable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ServerMemberRole role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    protected ServerMember() {
        // JPA
    }

    public ServerMember(Server server, User user, ServerMemberRole role) {
        this.server = server;
        this.user = user;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public Server getServer() {
        return server;
    }

    public User getUser() {
        return user;
    }

    public ServerMemberRole getRole() {
        return role;
    }

    public void setRole(ServerMemberRole role) {
        this.role = role;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMember other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
