# Architecture Decision Records

Short records of the non-obvious, durable decisions behind Roost, in the
[classic ADR](https://github.com/joelparkerhenderson/architecture-decision-record)
format. Each captures the context, the decision, and its consequences so the
reasoning survives beyond the pull request that introduced it.

| # | Decision | Status |
| --- | --- | --- |
| [0001](./0001-uuid-primary-keys.md) | UUID primary keys | Accepted |
| [0002](./0002-flyway-owns-schema.md) | Flyway owns the schema; JPA validates only | Accepted |
| [0003](./0003-timestamps-timestamptz-utc.md) | Timestamps as `timestamptz` in UTC | Accepted |
| [0004](./0004-stateless-jwt-security.md) | Stateless security, JWT authentication | Accepted |
