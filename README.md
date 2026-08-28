# Roost

Self-hosted, private communication platform for small trusted groups — text
channels, voice, video, and high-framerate screen sharing — delivered as a
desktop app.

> **Status:** early development · Phase 0 (foundation).

## Overview

Roost is a self-hosted alternative to mainstream chat/voice/video platforms,
built for privacy and full control. Your group runs its own server; members
install a desktop app and connect. No third-party account, no data handed to a
platform you don't own.

## Planned architecture

| Layer | Technology |
| --- | --- |
| Desktop app | Electron + React + TypeScript |
| Backend API | Java 21 + Spring Boot (REST + WebSocket) |
| Real-time media (voice/video/screen) | LiveKit (self-hosted SFU) |
| Persistence | PostgreSQL |
| Presence / pub-sub | Redis |
| Local development | Docker Compose |

The heavy real-time media path is handled by LiveKit; the backend stays light
(auth, channels, chat, presence, media tokens). See
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full design and roadmap.

## Security

This is a **public** repository. No secrets, credentials, tokens, private keys,
or infrastructure identifiers are ever committed. Configuration is provided at
runtime via environment variables (`.env`, which is git-ignored). Automated
secret scanning runs on every push and pull request. See
[`SECURITY.md`](SECURITY.md).

## Development

Setup instructions arrive with the backend and desktop scaffolds in later
phases. Contribution and commit conventions live in
[`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

[MIT](LICENSE) © João Zabarella
