# ADR-0001 — UUID primary keys

**Status:** Accepted · Phase 1

## Context

Roost's REST API exposes identifiers for users, servers, channels, and messages.
Sequential integer keys leak information (row counts, creation order) and invite
enumeration of resources by guessing adjacent ids. Keys are also referenced
across feature boundaries and, eventually, across services.

## Decision

Use `UUID` primary keys for all domain entities, stored in PostgreSQL `uuid`
columns.

- Hibernate assigns the value client-side (`GenerationType.UUID`) so an insert
  needs no extra round-trip to read a database-generated key back.
- Columns also default to `gen_random_uuid()` — a built-in core function since
  **PostgreSQL 13**, so no extension is required (Roost runs PostgreSQL 16). The
  default only fires for rows created outside JPA (raw SQL, future services),
  keeping the schema self-sufficient.

## Consequences

- Identifiers are non-sequential and safe to expose publicly; there is no
  enumeration or row-count signal.
- Keys can be generated anywhere without coordinating a sequence — useful once
  more than one writer exists.
- UUIDs are wider than `bigint` (16 bytes) and not monotonic, so index locality
  is slightly worse. At Roost's scale (a small group) this is negligible; if a
  hot, high-volume table ever needs better locality, a time-ordered UUID (v7)
  can be adopted per-table without changing the API contract.
