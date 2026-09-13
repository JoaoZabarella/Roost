# ADR-0005 — Argon2id password hashing and the invite model

**Status:** Accepted · Phase 1

## Context

Registration is closed: an account may only be created by redeeming an invite
(see [ADR-0004](./0004-stateless-jwt-security.md) for the auth model). This
raises two durable decisions. First, how account passwords are stored: the store
is the single most valuable target in a breach, and the hashing choice decides
how much an attacker gains from a database leak. Second, how invites are
modelled: they gate all registration, must not be forgeable or replayable beyond
their quota, and must not become usable secrets if the database leaks.

A registration invite is deliberately a different bounded context from the
server/channel invites that come in a later PR; this ADR covers only the former.

## Decision

**Passwords — Argon2id via a delegating encoder.** Passwords are hashed with
`Argon2PasswordEncoder` wrapped in a `DelegatingPasswordEncoder` that encodes
with Argon2id and can still verify other schemes (e.g. bcrypt). Each stored hash
carries its algorithm id as a prefix (`{argon2}`, `{bcrypt}`), so the scheme can
evolve without a migration. Argon2id is memory-hard: it forces an attacker's
per-guess cost to include memory, which blunts the GPU/ASIC parallelism that
makes cracking fast hashes cheap. The cost is paid on the server (CPU + RAM) at
login, which is rare in Roost's usage, so the latency is acceptable.

**Invites — hashed codes, atomic redemption, a policy seam.**

- The raw invite code is 256 bits of `SecureRandom` entropy and is **never
  stored**; only its **SHA-256** hash is. A plain hash is sufficient here — the
  code is already high-entropy and unguessable, unlike a human password — so a
  slow KDF would only add latency without adding safety.
- One model covers single-use and multi-use invites via `maxUses`/`uses`, plus
  `expiresAt` and `revoked`. An optional `email` binds an invite to one address.
- Redemption is a single conditional `UPDATE` that increments `uses` only while
  the invite is valid. Doing the check and the increment atomically makes it
  race-safe: concurrent redemptions serialize on the row lock, so a quota can
  never be exceeded. The whole registration runs in one transaction, so any later
  failure rolls the consumption back.
- Whether an invite is *required* is a `RegistrationPolicy` (Strategy): today
  `InviteOnlyRegistrationPolicy`; an open, public-registration policy can be
  added later without touching the service.
- The first account is solved by seeding a single-use `ADMIN` invite from
  `BOOTSTRAP_INVITE_CODE` when the users table is empty; the app fails fast if
  there is no user and no code, and never re-seeds once a user exists.

## Consequences

- A leaked user table yields Argon2id hashes that are expensive to crack, and
  leaked invite rows are only SHA-256 hashes — neither hands the attacker a
  usable secret.
- The password scheme can migrate in place: because each hash stores its
  algorithm prefix, a legacy hash still verifies, and an upgrade-on-login step
  (`DelegatingPasswordEncoder#upgradeEncoding`) can re-encode it to Argon2id when
  that path is wired up.
- Invite quotas and expiry are enforced correctly under concurrency without
  application-level locking.
- Registration modes are swappable behind one interface, keeping the private
  "closed today, open later" path a configuration choice rather than a rewrite.
- Costs: Argon2id spends server CPU/RAM per login (bounded by how rarely login
  happens here), and SHA-256 invite codes are unrecoverable — a lost code must be
  reissued, never looked up.
