# Security Policy

## Reporting a vulnerability

Please **do not** open a public issue for security problems. Use GitHub's
private vulnerability reporting instead:

1. Go to the **Security** tab of this repository.
2. Click **Report a vulnerability**.
3. Describe the issue and, if possible, steps to reproduce.

You will get a response as soon as reasonably possible.

## Secrets and credentials

- **No secrets are ever committed** to this repository (it is public).
- All secrets — database passwords, JWT signing keys, LiveKit API keys, TLS
  material — are supplied at runtime through environment variables.
- `.env` and related secret files are git-ignored; only `.env.example`
  (placeholder values) is tracked.
- Automated **secret scanning** (gitleaks) runs on every push and pull request.
  A push that introduces a detectable secret fails CI.

## Practices enforced in this project

- Least-privilege configuration; no default/hard-coded credentials.
- Dependencies reviewed on every pull request.
- Authentication tokens for real-time media are short-lived and issued
  server-side, never embedded in the client build.

## If a secret is ever exposed

1. Consider it compromised — rotate it immediately at the source.
2. Purge it from history if it was committed, and force-update.
3. Open a private advisory documenting scope and remediation.
