-- Phase 1 PR-B: authentication. Adds account-level role + email-verification
-- state to users, and the invite model (invites + redemption audit) behind
-- closed, invite-based registration.
--
-- Codes are stored only as their SHA-256 hex hash (64 chars); the raw code is
-- never persisted. Validity (revoked / quota / expiry) is enforced at
-- redemption time by an atomic conditional UPDATE in the application; the
-- CHECK constraints below are the last line of defence for the invariants.

ALTER TABLE users
    ADD COLUMN role varchar(16) NOT NULL DEFAULT 'MEMBER',
    ADD COLUMN email_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN verified_at timestamptz,
    ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'MEMBER'));

-- Argon2id hashes (with the DelegatingPasswordEncoder {argon2} prefix) run
-- ~100+ chars; the V2 varchar(100) is too tight. Widen with headroom.
ALTER TABLE users ALTER COLUMN password_hash TYPE varchar(255);

CREATE TABLE invites (
    id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    code_hash  varchar(64)  NOT NULL,
    email      varchar(254),
    role       varchar(16)  NOT NULL,
    max_uses   integer      NOT NULL DEFAULT 1,
    uses       integer      NOT NULL DEFAULT 0,
    expires_at timestamptz,
    revoked    boolean      NOT NULL DEFAULT false,
    created_by uuid,
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_invites PRIMARY KEY (id),
    CONSTRAINT uq_invites_code_hash UNIQUE (code_hash),
    CONSTRAINT ck_invites_role CHECK (role IN ('ADMIN', 'MEMBER')),
    CONSTRAINT ck_invites_max_uses CHECK (max_uses >= 1),
    -- uses stays within [0, max_uses]; the atomic consume relies on this.
    CONSTRAINT ck_invites_uses CHECK (uses >= 0 AND uses <= max_uses),
    CONSTRAINT fk_invites_created_by FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE invite_redemptions (
    id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    invite_id   uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    redeemed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_invite_redemptions PRIMARY KEY (id),
    CONSTRAINT uq_invite_redemptions_invite_user UNIQUE (invite_id, user_id),
    CONSTRAINT fk_invite_redemptions_invite FOREIGN KEY (invite_id)
        REFERENCES invites (id) ON DELETE CASCADE,
    CONSTRAINT fk_invite_redemptions_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

-- "Who joined through this invite" lookups.
CREATE INDEX idx_invite_redemptions_invite ON invite_redemptions (invite_id);
