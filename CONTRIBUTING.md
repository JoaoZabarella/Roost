# Contributing to Roost

## Commit convention

This project uses [Conventional Commits](https://www.conventionalcommits.org/).

```
<type>(optional scope): <short summary>

<optional body — the "why", not the "what">

<optional footer — BREAKING CHANGE:, refs, co-authors>
```

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `build`,
`perf`, `style`.

Examples:

```
feat(auth): issue short-lived LiveKit access tokens
fix(chat): prevent duplicate message on reconnect
chore(repo): add strict .gitignore and secret scanning
```

Keep the subject line ≤ 72 characters, imperative mood. Explain *why* in the
body when it is not obvious.

## Branches

Name branches `type/short-description`, e.g. `feat/text-channels`,
`chore/repo-foundation`, `fix/reconnect-dupe`.

Never commit directly to `main`; open a pull request.

## Pull requests

- One logical change per PR — keep them small and reviewable.
- Fill in the PR template, including the security checklist.
- All CI checks (including secret scanning) must pass before merge.

## Security — non-negotiable

- **Never** commit secrets, credentials, tokens, private keys, or real
  infrastructure identifiers. Use environment variables and `.env` (ignored).
- Before pushing, verify your diff contains no secrets. If in doubt, run a
  local scan (e.g. `gitleaks detect`).
- Report vulnerabilities privately — see [`SECURITY.md`](SECURITY.md).
