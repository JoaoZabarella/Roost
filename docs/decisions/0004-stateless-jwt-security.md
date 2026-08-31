# ADR-0004 — Stateless security, JWT authentication

**Status:** Accepted · Phase 1

## Context

The backend serves a desktop client over REST and WebSocket, and later issues
short-lived media tokens for LiveKit. Server-side HTTP sessions would mean
sticky sessions or a shared session store and complicate horizontal scaling and
the WebSocket/media paths. Registration is closed and invite-based, so the auth
surface is small and fully under our control.

## Decision

Run Spring Security **stateless** (`SessionCreationPolicy.STATELESS`) and
authenticate with a signed **JWT** (HS256, secret from `JWT_SECRET`). A
`OncePerRequestFilter` validates the `Authorization: Bearer` token per request
and populates the `SecurityContext`. The scaffold already disables the built-in
form/basic/logout mechanisms and fails closed with a plain `401` for
unauthenticated API calls; the JWT filter fills in the authentication itself in
PR-B. `/actuator/health` and `/actuator/info` stay public; everything else
requires authentication.

## Consequences

- No server-side session state: any instance can serve any request, which suits
  the WebSocket and future multi-instance deployment.
- Authentication travels with the request, so the same scheme covers REST and
  the STOMP/WebSocket handshake.
- Tokens are self-contained and short-lived; there is no server-side revocation
  list, so expiry is kept short and sensitive actions can be re-checked. A
  refresh-token flow can be added later without changing the stateless model.
- The signing secret is a mandatory environment variable with no default — the
  app refuses to boot without it.
