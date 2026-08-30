# Handoff / current state (not part of product docs — working notes)

Read this first when picking this project back up in a new session/tool
(Claude, Codex, whatever). Keeps continuity independent of any one tool's
memory system.

## What Roost is

Custom Discord-like desktop app for a small friend group. Stack: Electron +
React + TypeScript (client), Java 21 + Spring Boot (backend), LiveKit
self-hosted (WebRTC SFU — voice/video/screenshare up to 60fps),
PostgreSQL + Redis. Full architecture: `docs/ARCHITECTURE.md`.

## Hard constraints (non-negotiable, apply to every session/tool)

- **DOCKET isolation.** This repo/project is 100% personal, unrelated to
  DOCKET (user's employer). Never use DOCKET accounts, credentials,
  infrastructure, or tooling flows here — and never bring Roost-specific
  tooling (gstack, etc.) into DOCKET work. See
  `~/.ai-memory/` global memory and (Claude-side)
  `~/.claude/projects/-home-joaomuniz-discord-selfhost/memory/roost-docket-isolation.md`.
- **Identity isolation.** This repo uses a dedicated **personal** git identity,
  set as local `user.name`/`user.email` inside `~/Roost`, separate from the
  global git config (which stays DOCKET — never touch the global config from
  here). SSH uses a dedicated personal host alias with its own key that
  authenticates the personal GitHub account. The default `git@github.com`
  remote maps to the DOCKET identity — do not use it for this repo.
- **Commit attribution (must replicate in any tool).** Local git in `~/Roost`
  is set to `user.email = 145076656+JoaoZabarella@users.noreply.github.com`
  (GitHub noreply for account `JoaoZabarella`, id `145076656`). This (a) links
  every commit to the user's GitHub account and (b) keeps the real email out of
  the public history. Do **not** revert to a real-email identity. Commits are
  authored **solely by the user** — do **not** add `Co-Authored-By:` trailers
  for any AI tool. (Commits pushed before this was set still carry the old real
  email; left as-is by user decision — do not rewrite history.)
- **`gh` CLI scope.** Authenticated as `JoaoZabarella` via a fine-grained PAT
  scoped to **only** this repo (`Only select repositories → Roost`).
  Authorization to commit/open PRs is for **this repo only**, nowhere else.
- **Public repo, security-first.** Repo is public. Never commit secrets,
  tokens, keys, real infra identifiers. `.gitignore` is secrets-first
  already. Every push/PR is gitleaks-scanned (`.github/workflows/security.yml`).
  See `SECURITY.md`.
- **Workflow discipline.** Conventional Commits, one logical change per
  commit, small reviewable PRs per feature/fix/chore, PR template checklist
  filled honestly. See `CONTRIBUTING.md`.

## State as of 2026-08-28

- Repo created: `git@github.com:JoaoZabarella/Roost.git` (personal remote).
- **PR #1** (`chore/repo-foundation` → `main`): repo foundation — license,
  security docs, `.gitignore`, editorconfig, PR template, CI
  (`security.yml`: gitleaks on every push/PR + non-blocking dependency-review
  step), `docs/ARCHITECTURE.md`, README. All CI checks green. A legitimate
  Copilot review finding (secret-scan workflow only ran on `main`, not all
  branches) was fixed (`on.push` now has no branch filter). **PR #1 merged
  into `main` on 2026-08-29** (merge commit `0a455fc`).
- Branch ruleset (`main-protection`) for `main` was being set up by the user
  in the GitHub UI (require PR, require status checks, block force pushes,
  restrict deletions, enforcement Active) — **not confirmed submitted/active,
  verify.**
- "Dependency graph" repo setting (Settings → Security and analysis) was
  recommended so `dependency-review` can later become a real blocking gate
  once Java/Node manifests exist — **not confirmed enabled.**
- Stale duplicate Copilot comment on PR #1 diagnosed as already resolved
  (predates the fix commit) — user can re-request review to clear it if it
  still shows; not actioned.

## In progress: scoping gstack to this repo only

Decision: gstack (github.com/garrytan/gstack, Claude Code agent-role
toolkit) is useful for this project's dev workflow but must be **hard
blocked** from DOCKET work, not just discipline.

Plan (revised away from gstack "team mode" — that mode symlinks a
third-party tool into the repo tree, which is bad practice for a public
product repo):

1. **User runs, in their own terminal** (Claude's sandbox cannot touch
   `~/.bun` or `~/.ssh`):
   ```bash
   git clone --single-branch --depth 1 https://github.com/garrytan/gstack.git ~/.claude/skills/gstack
   cd ~/.claude/skills/gstack
   ./setup
   ```
   Decline telemetry/analytics prompt. Do **not** run `./setup --host codex`.
   Bun 1.4.0 already confirmed installed (`~/.bun/bin/bun`).
2. Configure a **PreToolUse hook** in `~/.claude/settings.json`: deny any
   Skill invocation named `gstack*` unless cwd is under `~/Roost`. This is
   the real enforcement boundary (gstack install itself is not sandboxed by
   Claude Code — the hook is what makes it repo-only).
3. Write `docs/gstack.md` in this repo: what gstack is, why scoped to this
   project only, install steps, DOCKET-isolation rule. Do not vendor
   gstack's code into the repo.
4. Commit + open **PR #2** for docs/gstack.md (+ hook is machine-local
   config, not committed).

**Status: steps 1–3 done.** gstack installed globally in basic mode
(`~/.claude/skills/gstack`, 55 skills, 76 browse commands; Playwright
Chromium install failed harmlessly — Node 18.19.1 on this machine, needs
Node 20+ for browse/automation commands only, core skills unaffected).
Hook written at `~/.claude/hooks/gstack-guard.sh` and registered in
`~/.claude/settings.json` under `PreToolUse` / matcher `Skill` — denies any
`gstack` or `gstack:*` skill unless cwd is under `~/Roost`; tested with 3
cases (blocked outside, allowed inside, unaffected for non-gstack skills),
all passed. `docs/gstack.md` written and committed. Step 4 (open PR) was
folded into PR #1 instead of a separate PR #2 — see below.

**Note on process deviation:** the original plan was a separate PR #2 for
gstack docs. In practice, both `docs/HANDOFF.md` and `docs/gstack.md` were
committed onto the still-open `chore/repo-foundation` (PR #1) branch instead
— branching a new PR off `main` before PR #1 merged would have missed all the
foundation work. **PR #1 has since merged into `main`**, so subsequent work
(backend scaffold, etc.) now branches cleanly from `main` and returns to
one-PR-per-feature.

## State as of 2026-08-30 (backend scaffold done; Phase 1 next)

All merged into `main`:

- **PR #2** (`chore/repo-foundation`): `docs/HANDOFF.md` + `docs/gstack.md`
  (gstack scoping notes). Copilot review comments resolved.
- **PR #3** (`feat/backend-scaffold`): Spring Boot backend scaffold — Gradle
  (Kotlin DSL), wrapper 8.10.2, Java 21, Spring Boot 3.5.4. Contents:
  `backend/RoostApplication.java` (entrypoint), `config/SecurityConfig.java`
  (stateless, health/info public, all else `authenticated()`, built-in
  login/basic/logout disabled, 401 entry point), `application.yml`
  (env-driven; `JWT_SECRET`/`POSTGRES_PASSWORD`/`REDIS_PASSWORD` have **no
  defaults** — fail fast), JPA `ddl-auto: validate` + Flyway
  (`V1__baseline.sql`, empty), Redis wiring, actuator health, multi-stage
  `Dockerfile` (non-root), `docker-compose.yml` (Postgres + Redis bound to
  `127.0.0.1` only; Redis `--requirepass`; LiveKit block commented for
  Phase 2), Testcontainers `contextLoads` test. `.env.example` extended with
  `REDIS_PASSWORD`.
- **PR #4** (`fix/testcontainers-docker-api`): pin `api.version=1.41` (Test JVM
  system prop, override `-PdockerApiVersion=`) so Testcontainers runs on
  modern Docker daemons (Engine 25+/API ≥ 1.40 rejected docker-java's 1.32
  fallback). `./gradlew test` now green (`tests=1, failures=0`).

**Verified locally:** `./gradlew clean build` green **with Docker running**;
`./gradlew bootJar` builds a ~68 MB fat jar. To boot the app you must first
`cp .env.example .env` and fill the secrets — e.g.
`openssl rand -base64 24` for `POSTGRES_PASSWORD`/`REDIS_PASSWORD` and
`openssl rand -base64 48` for `JWT_SECRET` — then `docker compose up -d`, then
`./gradlew bootRun` from `backend/` with those env vars exported. `.env` is
gitignored; never commit it.

**IDE note (not a code problem):** the Gradle project lives in `backend/`, not
the repo root. Opening `~/Roost` as a plain folder makes IntelliJ report
"Java file outside source root" and show no run gutter. Fix: open
`~/Roost/backend/build.gradle.kts` **as a project** (or right-click it →
Link/Import Gradle Project). Then IntelliJ sets source roots, attaches JDK 21,
and creates the `RoostApplication` run config.

**Build tooling caveat for any tool/sandbox:** `gradle` is **not** installed
globally — use the wrapper `./gradlew` from `backend/`. First run downloads the
Gradle distribution to `GRADLE_USER_HOME` (default `~/.gradle`). If a sandbox
blocks writes there, set `GRADLE_USER_HOME` to a repo-local, gitignored dir.

## Next: Phase 1 — auth + servers/channels + real-time text chat

Split into small, reviewable PRs (one logical change each). Decided design:
**UUID primary keys** (non-sequential; safe for public API), `timestamptz`
(UTC) timestamps, schema owned entirely by **Flyway** (`ddl-auto: validate`
stays — entities only map, never generate schema). Feature-package layout under
`com.roost`: `auth/`, `channel/`, `chat/`, `presence/` (+ existing `config/`).

### PR-A — domain model + migrations ✅ DONE (branch `feat/domain-migrations`, no endpoints)

**Status: implemented, `./gradlew test` green (5 tests), PR open.** JPA entities
under feature packages (`user/`, `server/`, `channel/`, `chat/`) + Spring Data
repositories + Flyway `V2__phase1_core.sql`. UUIDs assigned client-side
(`GenerationType.UUID`) with a `gen_random_uuid()` column default as a safety
net; `OffsetDateTime`↔`timestamptz`; enums as text + CHECK constraints; cascades
only server→channels→messages and server→members (user refs RESTRICT); composite
index `(channel_id, created_at)` for history pagination. `DomainPersistenceTest`
(`@DataJpaTest` + Testcontainers Postgres) proves validate + persistence + the
unique-membership constraint + pagination. **Next: start PR-B.**

Original scope (for reference):

JPA entities + Flyway `V2__phase1_core.sql`. Postgres `uuid` columns default
`gen_random_uuid()` — a **built-in core function since PostgreSQL 13**, so **no
extension is required** (we run PG16 in dev and tests; `pgcrypto`/`uuid-ossp`
are only needed on PG ≤ 12). Entities and tables:

- **User** — `id`, `username` (unique), `email` (unique), `password_hash`,
  `display_name`, `created_at`, `updated_at`.
- **Server** (community/guild) — `id`, `name`, `owner_id` → User, timestamps.
- **ServerMember** (N:N join + role) — `server_id` → Server, `user_id` → User,
  `role` (enum OWNER/ADMIN/MEMBER), `joined_at`; unique `(server_id, user_id)`.
- **Channel** — `id`, `server_id` → Server, `name`, `type` (enum TEXT/VOICE),
  `position`, timestamps.
- **Message** — `id`, `channel_id` → Channel, `author_id` → User, `content`,
  `created_at`, `edited_at` (nullable).

Indexes: unique `username`, unique `email`, unique `(server_id, user_id)`,
`(channel_id, created_at)` for message pagination. FKs `ON DELETE CASCADE`
where a child cannot outlive its parent (server → channels → messages;
server → members). No REST/service layer yet. Add repository interfaces
(`JpaRepository`) + a `@DataJpaTest` (or extend the Testcontainers test) to
prove the schema + mappings load. Acceptance: `./gradlew test` green.

### PR-B — authentication (invite-based, JWT)

Registration is **closed/invite-based** (known group, not public). Add: invite
model/flow, `POST /auth/register` (consumes a valid invite), `POST /auth/login`
→ signed JWT (HS256 with `JWT_SECRET`), a `OncePerRequestFilter` validating the
`Authorization: Bearer` token and populating the `SecurityContext`. Password
hashing with BCrypt (add `spring-boot-starter-security` is already present).
Wire the filter into `SecurityConfig` before the auth entry point; keep
`/actuator/health` public; protect everything else. Tests for register/login/
token validation.

### PR-C — servers/channels REST + real-time text chat (WebSocket/STOMP)

REST CRUD for servers/channels (create/list/join, permission-checked by
membership/role). Real-time chat over **STOMP on WebSocket**: connect with the
JWT, subscribe per channel, send/receive messages, persist to `Message`. Start
with Spring's simple in-memory broker; Redis relay comes with presence (PR-D).
Tests for REST + a WebSocket integration test.

### PR-D — presence via Redis

Online/offline + typing indicators using Redis pub/sub (the `spring-boot-
starter-data-redis` wiring already exists). Fan-out presence events; switch the
STOMP relay to Redis so multiple backend instances stay consistent. Note: the
dev `docker-compose` Redis now requires a password (`REDIS_PASSWORD`) — the app
reads `spring.data.redis.password`.

After Phase 1: Phase 2 (voice via LiveKit — uncomment the `docker-compose`
LiveKit block, add short-lived token minting endpoint) and onward per the
`docs/ARCHITECTURE.md` roadmap. The Electron + React desktop client is a
separate track (its own top-level folder, e.g. `desktop/`), not started yet.

## Working agreement for whoever continues (Claude, Codex, etc.)

- Before a new implementation: recommend model + reasoning effort, wait for the
  user's explicit `ok` before coding (user's standing preference).
- One logical change per commit; Conventional Commits; small PR per
  feature/fix; fill the PR template honestly; resolve Copilot review threads.
- Respect all Hard constraints above — especially DOCKET isolation, the
  commit-attribution identity, and the security-first/public-repo rules.
- Keep this file current: update **State** and **Next** when the next session
  would need it to continue.
- **Professional presentation (portfolio-grade).** This is a public repo the
  owner is proud of. Keep the history and PRs clean and mature: meaningful
  commit messages that explain the *why*, incremental logical steps (no giant
  dumps), honest PR descriptions, resolved review threads, green CI, and docs
  that a senior engineer would respect. Do **not** add AI-generated-by banners,
  co-author trailers, or similar noise. This is about the project genuinely
  *being* well-engineered — not about disguising how it was built or
  misrepresenting anyone's individual skill.
- **Decision notes per PR.** For each feature PR, include a short "why this
  decision" section (in the PR body and/or a brief note here) covering the
  non-obvious calls — e.g. why UUID PKs, why STOMP over raw WebSocket, why
  `ddl-auto: validate`, why fail-fast on secrets. Serves as documentation and
  as study material so the owner can defend every choice in a technical
  conversation.
