# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 73 — `chore/release-v1.3.1-prep` branch (doc sync ahead of the v1.3.1 release cut)

Version bump for the v1.3.1 patch release (fixes since `v1.3.0`: #78 Moshi export serialization, #80 import schema validation, #85 widget edge-to-edge). Confirmed `README.md` and `EARNIT_SPEC.md` were current before cutting, per the `chore/release-vX.Y.Z-prep` pattern established in Pass 69.

Most checklist sections are n/a — this is a version-bump-and-doc-check pass, no source changes beyond `app/build.gradle.kts`.

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility / Deprecated APIs / Dev Seed Data — n/a
No source code touched this pass.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only `app/build.gradle.kts` and this log changed.

#### Spec Review ✅ (checked, no drift found)
Recomputed test counts against a fresh `@Test` grep, filtering the one false-positive substring match (`@TestInstallIn` in `TestAppModule.kt`): 220 unit tests / 28 files, 122 instrumented tests / 32 files. Matches `EARNIT_SPEC.md`'s documented "220 unit tests across 28 test files. 120 instrumented tests across 32 files" (120 is `TESTING.md`'s intentional round-to-nearest-10 for the instrumented aggregate, not staleness) and `README.md`'s "220+ / 120+" badge and prose. No feature-list or SDK-version drift found in either doc. Nothing to fix.

#### Tests — n/a
No test files changed. Pre-release gate already run on the merged `fix/widget-edge-to-edge` branch (Pass 72): `test` 220/220, `connectedDebugAndroidTest` 122/122.

#### Release
Version bump for the v1.3.1 cut: `app/build.gradle.kts` `versionCode` 4→5, `versionName` "1.3.0"→"1.3.1", per `DEV_PLAYBOOK.md`'s release process.

---

### Pass 74 — `feat/about-disclaimer` branch

Added a short liability disclaimer ("as is," no warranty, not liable for data loss/damages) to the About screen, directly below the Privacy policy row, so the app carries the same baseline protection bigger free apps ship with.

#### Duplication / Decoupling / Hardcoded Values / Deprecated APIs / Dev Seed Data — n/a
Two new `Strings.kt` constants and one new static text `Column`, copied structurally from the existing "The idea" section (same `Column`/`Text` pattern, `HorizontalDivider()` separator) — no new abstraction, no business logic, no colors/durations introduced.

#### Complexity & Pattern Health ✅ (checked, pre-existing issue noted, not fixed)
`AboutScreen`'s top-level composable was already ~200 lines before this change (over the ~150-line split guideline) — this pass adds 16 lines to it rather than introducing the overage. Not restructured here: splitting it is a separate refactor with its own blast radius, out of scope for a copy-only addition.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only the 5 intended files changed. No unused imports — the new code reuses `Column`/`Text`/`HorizontalDivider`/`MaterialTheme` already imported in the file.

#### Naming Consistency ✅ (checked)
`ABOUT_DISCLAIMER_TITLE`/`ABOUT_DISCLAIMER_BODY` follow the existing `ABOUT_*` naming and sit next to `ABOUT_PRIVACY_*` in `Strings.kt`.

#### Accessibility ✅ (checked, n/a)
Static text, not tappable — no content description or touch-target sizing applies.

#### Spec Review ✅ — updated
`EARNIT_SPEC.md`'s About screen tree line didn't mention the Privacy policy link either; added both Privacy policy and Disclaimer to that line so it now matches the actual screen contents.

#### Tests ✅ — added, ran on device
Added `aboutScreen_displaysDisclaimer` to `SettingsScreenUiTest.kt`, mirroring the existing `aboutScreen_doesNotClaimDataNeverLeavesThePhone` regression-style assertion. Ran the full file on a connected emulator before committing: 15/15 passed. `TESTING.md`'s `SettingsScreenUiTest` row count (14→15) and its UI-layer pyramid figure (87→88) updated to match; the instrumented-aggregate "120 tests" headers stay as-is (already an intentional round-to-nearest-10, per Pass 73). No `AppModule`/`TestAppModule`/`@Inject` change, so `assembleDebugAndroidTest` wasn't required beyond the `connectedDebugAndroidTest` run. `./gradlew ktlintCheck` and `assembleDebug` both pass.

---

### Pass 75 — `chore/dependency-version-bump` branch

Google Play compliance/upgrade audit found `androidx.lifecycle` (2.6.2) and `androidx.navigation:navigation-compose` (2.7.3) in `gradle/libs.versions.toml` several versions behind the rest of the catalog. Bumped `lifecycle` → 2.10.0, `navigationCompose` → 2.9.8, and `work` → 2.12.0 (this last one wasn't stale, just one minor release behind — bumped while touching the file).

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility / Dev Seed Data — n/a
Version-catalog-only change, no source code touched.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only `gradle/libs.versions.toml` and docs changed.

#### Deprecated APIs ✅ — investigated, documented as a version ceiling
Latest stable `lifecycle` (2.11.0) and `navigation-compose` (2.10.2) both require `compileSdk 37`, which fails `checkDebugAarMetadata` against this project's `compileSdk 36`. Took the highest versions still compatible with `compileSdk 36` (2.10.0 and 2.9.8 respectively) instead, mirroring the existing `core-ktx` 1.18.x constraint. Documented as a new gotcha (#7) in `DEV_PLAYBOOK.md`'s Tooling Upgrade Reference so the next bump doesn't rediscover it by trial and error.

#### Spec Review ✅ (checked, n/a)
No behavior change — `EARNIT_SPEC.md` doesn't document dependency versions.

#### Tests ✅ (checked, n/a)
No source change, so no new test warranted. Ran the full pre-merge gate sequentially (never in parallel, per `CLAUDE.md`'s Windows Kotlin-daemon note): `./gradlew ktlintCheck`, `./gradlew test` (passed), `./gradlew assembleDebug` (passed), `./gradlew assembleDebugAndroidTest` (passed — confirms the Hilt graph still resolves against the bumped `hilt-navigation-compose`/`hilt-work` transitive deps).
