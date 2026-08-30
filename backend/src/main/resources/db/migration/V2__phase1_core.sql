-- Phase 1 core domain: users, servers, memberships, channels, messages.
--
-- UUID primary keys default to gen_random_uuid() (built-in core since
-- PostgreSQL 13 — no extension needed on our PG16). The application also
-- assigns the UUID client-side via Hibernate; the default only fires for rows
-- created outside JPA (raw SQL, future services). All timestamps are
-- timestamptz (UTC). Enums are stored as text with CHECK constraints, matching
-- the JPA EnumType.STRING mappings. FKs cascade only where a child cannot
-- outlive its parent (server -> channels -> messages; server -> members);
-- references to a user (owner, member, author) RESTRICT so an account in use
-- cannot be deleted out from under live rows.

CREATE TABLE users (
    id            uuid        NOT NULL DEFAULT gen_random_uuid(),
    username      varchar(32) NOT NULL,
    email         varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    display_name  varchar(64) NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE servers (
    id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    name       varchar(100) NOT NULL,
    owner_id   uuid         NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_servers PRIMARY KEY (id),
    CONSTRAINT fk_servers_owner FOREIGN KEY (owner_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_servers_owner ON servers (owner_id);

CREATE TABLE server_members (
    id        uuid        NOT NULL DEFAULT gen_random_uuid(),
    server_id uuid        NOT NULL,
    user_id   uuid        NOT NULL,
    role      varchar(16) NOT NULL,
    joined_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_server_members PRIMARY KEY (id),
    CONSTRAINT uq_server_members_server_user UNIQUE (server_id, user_id),
    CONSTRAINT ck_server_members_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT fk_server_members_server FOREIGN KEY (server_id)
        REFERENCES servers (id) ON DELETE CASCADE,
    CONSTRAINT fk_server_members_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

-- The unique (server_id, user_id) constraint already indexes server_id-leading
-- lookups; add the reverse for "servers this user belongs to".
CREATE INDEX idx_server_members_user ON server_members (user_id);

CREATE TABLE channels (
    id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    server_id  uuid         NOT NULL,
    name       varchar(100) NOT NULL,
    type       varchar(16)  NOT NULL,
    position   integer      NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_channels PRIMARY KEY (id),
    CONSTRAINT ck_channels_type CHECK (type IN ('TEXT', 'VOICE')),
    CONSTRAINT fk_channels_server FOREIGN KEY (server_id)
        REFERENCES servers (id) ON DELETE CASCADE
);

CREATE INDEX idx_channels_server ON channels (server_id);

CREATE TABLE messages (
    id         uuid          NOT NULL DEFAULT gen_random_uuid(),
    channel_id uuid          NOT NULL,
    author_id  uuid          NOT NULL,
    content    varchar(4000) NOT NULL,
    created_at timestamptz   NOT NULL DEFAULT now(),
    edited_at  timestamptz,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT fk_messages_channel FOREIGN KEY (channel_id)
        REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_author FOREIGN KEY (author_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

-- Channel history pagination: newest-first scan per channel.
CREATE INDEX idx_messages_channel_created ON messages (channel_id, created_at);
CREATE INDEX idx_messages_author ON messages (author_id);
