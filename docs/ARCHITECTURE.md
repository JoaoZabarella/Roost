# Roost — Architecture & Roadmap

## Goal

A self-hosted, private communication platform for a small trusted group:
text channels, voice, video, and high-framerate (up to 60 fps) screen sharing,
delivered as an installable desktop application.

## System overview

```
        ┌──────────────────────────┐
        │   Desktop app (Electron) │  React + TypeScript
        │   - UI, chat, presence   │
        │   - WebRTC client        │
        └───────┬───────────┬──────┘
                │ REST/WS   │ WebRTC (media)
                ▼           ▼
     ┌────────────────┐   ┌──────────────────────┐
     │  Backend API   │   │   LiveKit (SFU)       │
     │  Java 21 +     │   │  voice / video /      │
     │  Spring Boot   │   │  screenshare, 60 fps  │
     │  REST + WS     │   └──────────────────────┘
     │  - auth        │           ▲
     │  - channels    │  issues short-lived
     │  - chat        │  access tokens
     │  - presence    │───────────┘
     └───┬───────┬────┘
         │       │
         ▼       ▼
   ┌──────────┐ ┌────────┐
   │ Postgres │ │ Redis  │
   │  data    │ │presence│
   └──────────┘ └────────┘
```

### Responsibilities

- **Desktop app (Electron + React + TS):** all UI, text chat, presence display,
  and the WebRTC client that joins LiveKit rooms. Uses Electron's
  `desktopCapturer` for screen sharing.
- **Backend (Java 21 + Spring Boot):** authentication, servers/channels model,
  persistent text chat over WebSocket, presence, and — critically — issuing
  **short-lived LiveKit access tokens**. Media never flows through the backend,
  so its load stays light.
- **LiveKit (self-hosted SFU):** the heavy real-time path. Selective forwarding
  (no server-side mixing) keeps CPU reasonable and supports high-fps screen
  share.
- **PostgreSQL:** users, channels, messages.
- **Redis:** presence and pub/sub for real-time fan-out.

## Security model

- Media access tokens are **short-lived** and minted **server-side**; the
  client build contains no long-lived secrets.
- All secrets are runtime environment variables; nothing sensitive is committed
  (public repo). Secret scanning gates every push/PR.
- Registration is closed/invite-based — the server is for a known group, not the
  public internet.
- TLS terminates at a reverse proxy in front of the backend; LiveKit uses its
  own secured transport.

## Roadmap (phased)

| Phase | Deliverable |
| --- | --- |
| **0** | Foundation: repo, security baseline, tooling, CI. |
| **1** | Auth + servers/channels model + real-time text chat (WebSocket). |
| **2** | Voice channels via LiveKit (join/leave, mute, speaking indicator). |
| **3** | Video + screen sharing, tuned for high framerate/bitrate. |
| **4** | UI polish, presence, notifications, theming. |
| **5** | Desktop packaging (installers Win/macOS/Linux) + auto-update. |
| **6** | Public deployment of backend + LiveKit for remote members. |

Each phase ships as one or more small, reviewable pull requests.

## Hosting reality

Development runs entirely locally (Docker Compose) at no cost. Only Phase 6 —
letting remote members connect — needs a public server. For a handful of
members the bandwidth is modest and a small VPS (low-latency region) suffices;
this is deliberately deferred until the app is worth deploying.
