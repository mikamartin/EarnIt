# CLAUDE.md

Project-specific instructions for AI assistants working in this repository.

## What this is

EarnIt — a local-only, gamified productivity Android app (Kotlin, Jetpack Compose, Room, Hilt). See [docs/EARNIT_SPEC.md](docs/EARNIT_SPEC.md) for the full spec.

This repo is also a portfolio piece. It's built with heavy AI assistance, and this file exists so the rules of engagement are checked into the repo itself rather than living only in an external memory store — both as working instructions and as a transparent record of how AI was used here. See the README's "AI-assisted development workflow" section for the portfolio-facing summary.

## Collaboration rules

- Confirm before making non-trivial changes, and explain the rationale for technical and UX decisions before implementing.
- Never merge a branch or PR without the user's explicit go-ahead. Commit, present a summary of what changed and why, then stop and wait.
- Branch naming follows the existing convention: `type/topic` (e.g. `feature/ui-tests`, `fix/about-screen-design-review`, `chore/ktlint-setup`).
- Before the repo has a GitHub remote: one local feature branch per logical unit of work, merged locally only once the user says go.
- Once the repo is published: open a PR per unit of work instead of merging locally. The user reviews and merges PRs themselves on GitHub.

## Source-of-truth docs (`docs/`)

- [EARNIT_SPEC.md](docs/EARNIT_SPEC.md) — what the app does, architecture, deferred ideas
- [TESTING.md](docs/TESTING.md) — test strategy, coverage, cadence
- [MANUAL_TEST_PLAN.md](docs/MANUAL_TEST_PLAN.md) — the journeys that are deliberately manual-only, not just deferred
- [DEV_PLAYBOOK.md](docs/DEV_PLAYBOOK.md) — post-work cleanup checklist, ship checklist, tooling upgrade reference
- [CLEANUP_LOG.md](docs/CLEANUP_LOG.md) — log of every cleanup pass, in order
- [CLOSED_TESTING_GUIDE.md](docs/CLOSED_TESTING_GUIDE.md) — plain-language testing guide for Play closed testing recruits

## Commit messages

Follows [Conventional Commits](https://www.conventionalcommits.org/).

**Format:** `type: short description` — subject line ≤ 72 characters, imperative mood ("Add" not "Added"). Subject line only — no body. Context belongs in the branch name, PR description, or CLEANUP_LOG.md.

**Types:**
- `feat` — new feature or user-visible behaviour change
- `fix` — bug fix
- `docs` — documentation only (README, spec, playbook, etc.)
- `chore` — tooling, build, dependencies, config — no production code change
- `refactor` — code restructure with no behaviour change
- `test` — adding or updating tests only

## Working agreements

- After any significant feature work, walk through steps in `DEV_PLAYBOOK.md` §1 Post-Work Cleanup and log a new pass in `CLEANUP_LOG.md`.
- Keep `EARNIT_SPEC.md` in sync with what was actually built — update it when implementation diverges from the spec intentionally; if it diverged unintentionally, that's a bug to fix, not a spec update.
- Flag `DEV_PLAYBOOK.md` ship checklist items if a task touches them, and strike resolved items out entirely — the checklist should only ever contain open work.

## Commands

- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedDebugAndroidTest` (requires a device/emulator — see `docs/TESTING.md` Deferrals for a known Android 16 compatibility gap)
- Lint check / autofix: `./gradlew ktlintCheck` / `./gradlew ktlintFormat`
- Debug build: `./gradlew assembleDebug`
