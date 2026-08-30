# ADR-0002 — Flyway owns the schema; JPA validates only

**Status:** Accepted · Phase 1

## Context

With JPA/Hibernate, the schema can be produced two ways: let Hibernate generate
DDL from the entities (`ddl-auto: update/create`), or manage it explicitly with
versioned migrations. Generated DDL is convenient early but drifts silently,
is not reviewable, and cannot express everything the database should enforce
(named constraints, partial indexes, `CHECK`s, precise types).

## Decision

Flyway is the single source of truth for the schema. Migrations under
`db/migration` (`V1__…`, `V2__…`) create and evolve every table, constraint, and
index. Hibernate runs with `ddl-auto: validate`: it maps entities to the
existing schema and **fails fast at startup** if a mapping and the schema
disagree, but it never issues DDL.

## Consequences

- Schema changes are explicit, versioned, code-reviewed, and identical across
  every environment.
- A mapping that drifts from the database is caught at boot, not in production.
- The database can carry integrity rules the ORM does not model (e.g. enum
  `CHECK` constraints, cascade rules), and they are guaranteed to exist.
- Cost: every schema change requires a migration by hand — the intended
  trade-off, and the reason the schema stays trustworthy.
