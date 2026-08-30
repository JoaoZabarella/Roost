package com.roost.server;

/**
 * A member's authority within a single server. Persisted as its name
 * ({@code EnumType.STRING}) so ordering changes never corrupt existing rows.
 */
public enum ServerMemberRole {
    OWNER,
    ADMIN,
    MEMBER
}
