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
- **Identity isolation.** Local git config in `~/Roost` is
  `João Zabarella <jpzmuniz@gmail.com>`. Global git config stays DOCKET —
  never touch it from here. SSH: personal alias `github-personal` (key
  `~/.ssh/roost_ed25519`) authenticates as `JoaoZabarella`. Default
  `git@github.com` remains the DOCKET identity — do not use it for this repo.
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
  branches) was fixed (`on.push` now has no branch filter). **Merge status
  of PR #1 not yet confirmed by user — check before assuming it's in
  `main`.**
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
— branching a new PR off `main` before PR #1 merges would miss all the
foundation work. Once PR #1 merges, subsequent work (backend scaffold, etc.)
should branch cleanly from `main` and go back to one-PR-per-feature.
**Recommend merging PR #1 soon** to unblock normal branching.

## Next after gstack scoping

- PR #3: Java/Spring Boot backend scaffold + docker-compose dev env
  (Postgres/Redis/LiveKit).
- PR #4: Electron + React desktop app scaffold.
- Then continue phases per `docs/ARCHITECTURE.md` roadmap table (2–6).
