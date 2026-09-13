package com.roost.user;

/**
 * Account-wide authority, distinct from a per-server {@code ServerMemberRole}.
 * {@code ADMIN} is the small-group operator (mints invites, later admin tasks);
 * {@code MEMBER} is an ordinary account. Persisted as its name
 * ({@code EnumType.STRING}) so ordering changes never corrupt existing rows.
 */
public enum SystemRole {
    ADMIN,
    MEMBER
}
