# ADR-0003 — Timestamps as `timestamptz` in UTC

**Status:** Accepted · Phase 1

## Context

Roost records creation, update, edit, and join times that will be compared,
sorted, and rendered to clients in different time zones. PostgreSQL offers
`timestamp` (no zone) and `timestamptz` (an absolute instant). On the Java side,
`java.time.Instant` maps by default to a zoneless JDBC timestamp, while
`OffsetDateTime` maps to a zoned one — a mismatch would fail `ddl-auto: validate`
(see [ADR-0002](./0002-flyway-owns-schema.md)).

## Decision

Store all timestamps as `timestamptz`, map them to `OffsetDateTime` in the
entities, and pin the session time zone to UTC via
`hibernate.jdbc.time_zone: UTC`. Persistence-managed times use Hibernate's
`@CreationTimestamp` / `@UpdateTimestamp`.

## Consequences

- Every stored time is an unambiguous absolute instant; no "which zone was this?"
  ambiguity, and comparisons/ordering are correct.
- The `OffsetDateTime` ↔ `timestamptz` pairing validates cleanly, keeping the
  fail-fast guarantee from ADR-0002 intact.
- Presentation-time zone conversion is the client's responsibility, which is
  where the user's locale actually lives.
