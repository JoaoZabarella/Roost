# Roost — Backend Delivery Plan

Complements the high-level phase table in
[`ARCHITECTURE.md`](./ARCHITECTURE.md) with the concrete, PR-level plan for the
backend. Every phase ships as small, reviewable pull requests, one logical
change each.

## Working conventions

- **Conventional Commits**, one logical change per commit; small PRs per
  feature/fix.
- Each feature PR states the *why* behind non-obvious calls; durable decisions
  are recorded as [ADRs](./decisions).
- **Flyway owns the schema.** JPA runs with `ddl-auto: validate` — entities map
  the schema, never generate it (see [ADR-0002](./decisions/0002-flyway-owns-schema.md)).
- **Secrets are environment variables with no defaults** (`JWT_SECRET`,
  `POSTGRES_PASSWORD`, `REDIS_PASSWORD`) so the app fails fast rather than
  booting on a weak fallback. Nothing sensitive is committed; secret scanning
  gates every push.

## Phase 1 — auth, servers/channels, real-time text chat

Design baseline for the phase: **UUID primary keys**
([ADR-0001](./decisions/0001-uuid-primary-keys.md)), `timestamptz` (UTC)
timestamps ([ADR-0003](./decisions/0003-timestamps-timestamptz-utc.md)), and a
feature-package layout under `com.roost` (`user/`, `server/`, `channel/`,
`chat/`, `auth/`, `presence/`, `config/`).

### PR-A — domain model + migrations ✅

JPA entities (`User`, `Server`, `ServerMember`, `Channel`, `Message`) with their
Spring Data repositories and Flyway `V2__phase1_core.sql`. Enums are stored as
text with `CHECK` constraints; foreign keys cascade only where a child cannot
outlive its parent (server → channels → messages; server → members), while
references to a user (owner, member, author) `RESTRICT`. A composite index on
`(channel_id, created_at, id)` backs stable newest-first history pagination.
Verified by a `@DataJpaTest` running against a real PostgreSQL (Testcontainers),
which proves the mappings validate against the migrated schema.

### PR-B — authentication (invite-based, JWT)

Registration is closed/invite-based — Roost serves a known group, not the public
internet. Adds the invite model and flow, `POST /auth/register` (consumes a
valid invite), `POST /auth/login` returning a signed JWT (HS256), and a
`OncePerRequestFilter` that validates the `Authorization: Bearer` token and
populates the `SecurityContext`. Passwords hashed with BCrypt. The filter slots
into the existing stateless `SecurityConfig`
([ADR-0004](./decisions/0004-stateless-jwt-security.md)); `/actuator/health`
stays public, everything else requires authentication.

### PR-C — servers/channels REST + real-time text chat

REST CRUD for servers and channels (create/list/join, permission-checked by
membership and role). Real-time chat over **STOMP on WebSocket**: clients connect
with their JWT, subscribe per channel, and send/receive messages that persist to
`Message`. Starts on Spring's in-memory simple broker; the Redis relay arrives
with presence.

### PR-D — presence via Redis

Online/offline and typing indicators via Redis pub/sub. Presence events fan out
through Redis, and the STOMP relay moves onto Redis so multiple backend
instances stay consistent.

## Beyond Phase 1

Phase 2 introduces voice channels via a self-hosted LiveKit SFU (join/leave,
mute, speaking indicator) with short-lived access tokens minted server-side;
later phases add video and high-framerate screen sharing, presence/notification
polish, and desktop packaging. See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for the
full phase table. The Electron + React desktop client is a separate track (its
own top-level module), not yet started.
