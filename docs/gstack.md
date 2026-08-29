# gstack (dev workflow tooling)

This project uses [gstack](https://github.com/garrytan/gstack) — an
MIT-licensed Claude Code skill suite (roles/slash-commands like
`/office-hours`, `/autoplan`, `/plan-eng-review`, `/review`, `/qa`,
`/ship`, etc.) — to assist with planning, review, and shipping work on
**this repository only**.

## Scope: Roost only, never anywhere else

gstack is installed **globally** for the machine (Claude Code skills are not
per-repo), but its use is restricted to this project:

- Installed in **basic mode** (no repo symlinks / "team mode" vendoring —
  gstack's code is not, and must never be, committed into this repo).
- A **`PreToolUse` hook**, configured machine-side in
  `~/.claude/settings.json` (not part of this repo — it's local Claude Code
  config), denies any `gstack` or `gstack:*` skill invocation unless the
  current working directory is under `~/Roost`. This is the actual
  enforcement boundary, not just a convention.
- gstack must **never** be installed or used for the user's employer
  (DOCKET) work, in any repo, tool, or account. This repo and that work are
  fully unrelated and must stay isolated — see `docs/HANDOFF.md`.
- gstack is a **Claude Code** tool. It is intentionally not installed for
  Codex (`./setup --host codex` was skipped) — Codex sessions on this repo
  don't get it, by choice.

## Install (for reference — already done)

```bash
git clone --single-branch --depth 1 https://github.com/garrytan/gstack.git ~/.claude/skills/gstack
cd ~/.claude/skills/gstack
./setup
```

Telemetry/analytics prompt: declined. `--host codex` flag: not used.

Note: gstack's setup also tries to install a Playwright Chromium browser for
its browse commands; that step requires Node.js 20+ and will fail (harmlessly)
on older Node versions. It only affects the browse/automation commands, not
the core skill set — safe to ignore until/unless those commands are needed.

## Why this exists

The repo is public. Pulling in third-party agent tooling is convenient for
development but must not leak into unrelated (especially employer) work, and
must not end up vendored into a public product repo. Global install +
hard hook-based scoping keeps both invariants true without relying on
remembering to be careful.
