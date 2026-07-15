# CLAUDE.md

Project-specific instructions for AI assistants working in this repository.

## What this is

EarnIt — a local-only, gamified productivity Android app (Kotlin, Jetpack Compose, Room, Hilt). See [docs/EARNIT_SPEC.md](docs/EARNIT_SPEC.md) for the full spec.

This repo is also a portfolio piece. It's built with heavy AI assistance, and this file exists so the rules of engagement are checked into the repo itself rather than living only in an external memory store — both as working instructions and as a transparent record of how AI was used here. See the README's "AI-assisted development workflow" section for the portfolio-facing summary.

## Collaboration rules

- Confirm before making non-trivial changes, and explain the rationale for technical and UX decisions before implementing.
- Always confirm the branch name and proposed commit message with the user before committing — never commit without explicit approval of both.
- Never merge a branch or PR without the user's explicit go-ahead. Commit, present a summary of what changed and why, then stop and wait.
- Branch naming follows the convention: `type/topic` (e.g. `feature/ui-tests`, `fix/about-screen-design-review`, `chore/ktlint-setup`). The topic should describe the work, not the date or session — no timestamps in branch names.
- Before the repo has a GitHub remote: one local feature branch per logical unit of work, merged locally only once the user says go.
- Once the repo is published: open a PR per unit of work instead of merging locally. The user reviews and merges PRs themselves on GitHub.

## Branch and PR workflow (post-publish SOP)

1. **Create a branch** — `git checkout -b type/topic` from `main`
2. **Make changes** — confirm rationale with user before non-trivial changes
3. **Test locally** — run the relevant commands:
   - Lint: `./gradlew ktlintCheck`
   - Unit tests: `./gradlew test`
   - Build: `./gradlew assembleDebug`
4. **Human reviews diff and does exploratory/manual testing** — inspect the changes, run the app, test affected flows
5. **Commit** — one subject-line commit per logical change (`type: description`)
6. **Push** — `git push -u origin branch-name`
7. **Open PR** — `gh pr create` with a Conventional Commits-style title (see below) and a summary; no "Generated with Claude Code" footer; CI runs automatically
8. **Human reviews on GitHub and merges** — never merge without explicit go-ahead
9. **Pull and clean up locally** — `git checkout main && git pull origin main && git branch -d branch-name`

## Source-of-truth docs (`docs/`)

- [EARNIT_SPEC.md](docs/EARNIT_SPEC.md) — what the app does, architecture, deferred ideas
- [TESTING.md](docs/TESTING.md) — test strategy, coverage, cadence
- [MANUAL_TEST_PLAN.md](docs/MANUAL_TEST_PLAN.md) — the journeys that are deliberately manual-only, not just deferred
- [DEV_PLAYBOOK.md](docs/DEV_PLAYBOOK.md) — ship checklist, release process, tooling upgrade reference
- [CLEANUP_RULES.md](docs/CLEANUP_RULES.md) — post-work cleanup checklist and log retention rule
- [CLEANUP_LOG.md](docs/CLEANUP_LOG.md) — the 3 most recent cleanup passes
- [CLOSED_TESTING_GUIDE.md](docs/CLOSED_TESTING_GUIDE.md) — plain-language testing guide for Play closed testing recruits

## Commit and PR titles

Follows [Conventional Commits](https://www.conventionalcommits.org/).

**Format:** `type: short description` — subject line ≤ 72 characters, imperative mood ("Add" not "Added"). Subject line only — no body. Context belongs in the branch name, PR description, or CLEANUP_LOG.md.

**PR titles use the same format as commit subjects** — same types, same length and mood rules.

**Types:**
- `feat` — new feature or user-visible behaviour change
- `fix` — bug fix
- `docs` — documentation only (README, spec, playbook, etc.)
- `chore` — tooling, build, dependencies, config — no production code change
- `refactor` — code restructure with no behaviour change
- `test` — adding or updating tests only

## Working agreements

- Code comments and doc updates (`DEV_PLAYBOOK.md`, `TESTING.md`, `EARNIT_SPEC.md`, etc.) describe the current state and behaviour, not the history of what was wrong or changed to get here. State the rule or behaviour directly; don't narrate the prior bug or implementation. Keep it concise. (`CLEANUP_LOG.md` is the exception — it's a retrospective record by design.)
- After any significant feature work, walk through the checklist in `CLEANUP_RULES.md` and log a new pass in `CLEANUP_LOG.md`.
- Keep `EARNIT_SPEC.md` in sync with what was actually built — update it when implementation diverges from the spec intentionally; if it diverged unintentionally, that's a bug to fix, not a spec update.
- Flag `DEV_PLAYBOOK.md` ship checklist items if a task touches them, and strike resolved items out entirely — the checklist should only ever contain open work.

## Commands

- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedDebugAndroidTest` (requires a device/emulator — see `docs/TESTING.md` Deferrals for a known Android 16 compatibility gap)
- Hilt/DI graph validation (no device needed): `./gradlew assembleDebugAndroidTest` — compiles the androidTest variant and validates the full Hilt graph, including `TestAppModule`. Run this whenever a change touches `AppModule`, `TestAppModule`, or adds a new `@Inject` site — `test` and `assembleDebug` don't compile the androidTest source set, so a binding that's missing only from `TestAppModule` won't surface otherwise.
- Lint check / autofix: `./gradlew ktlintCheck` / `./gradlew ktlintFormat`
- Debug build: `./gradlew assembleDebug`

**Important — never run Gradle tasks in parallel.** Launching `ktlintCheck`, `assembleDebug`, and `test` as concurrent background commands causes multiple Kotlin daemon sessions to collide on the same incremental build cache files (Windows `AccessDeniedException`), leaving the build in a broken state that requires `./gradlew clean` to recover. Always run build, lint, and test sequentially.
