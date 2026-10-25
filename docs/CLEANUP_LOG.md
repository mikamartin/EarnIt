# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 72 — `fix/widget-edge-to-edge` branch

Play Console's pre-launch report on the shipped `v1.3.0` build flagged edge-to-edge issues. Investigation found `MainActivity` already correctly calls `enableEdgeToEdge()`; the real gap was `WidgetConfigActivity` and `WidgetTaskLogActivity` never calling it, unlike `MainActivity`, despite both already padding content with `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`.

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values — n/a
Two-line diff (one import, one call) in each of two files, mirroring `MainActivity.kt`'s existing pattern exactly. No new logic, no new abstraction.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only the two intended widget-activity files changed. `ktlintCheck` clean.

#### Accessibility ✅ (checked, n/a)
No new touch targets or content descriptions introduced.

#### Deprecated APIs ✅ — investigated, documented as not actionable
The report's second warning (`setStatusBarColor`/`setNavigationBarColor`/`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`) traces to obfuscated AndroidX classes, not app code — confirmed via full-codebase search (no direct calls, no Accompanist dependency). These are `androidx.activity`'s own `enableEdgeToEdge()` fallback on API 31–34 (`minSdk` 31, OS-native edge-to-edge only from API 35) — not fixable without dropping support for Android 12–14. Documented as a permanent constraint in `DEV_PLAYBOOK.md`'s Known Limitations so it isn't re-investigated on the next pre-launch report.

#### Spec Review ✅ (checked, n/a)
`EARNIT_SPEC.md` doesn't document edge-to-edge/window-inset handling at this level of detail for the widget activities — nothing to update.

#### Tests ✅ (checked, n/a)
Window-flag-only change with no new business logic; no new unit test warranted. Ran the full suite as the pre-release gate this branch feeds into: `./gradlew test` 220/220, `./gradlew connectedDebugAndroidTest` on the API 36 emulator 122/122 (0 skipped, 0 failed), `./gradlew assembleDebugAndroidTest` (Hilt graph, run as routine gate — no `AppModule`/`TestAppModule`/`@Inject` change). Manually verified both activities render with correct status-bar icon contrast and no content overlap (screenshots on the API 36 emulator), per `MANUAL_TEST_PLAN.md`'s widget-cadence step 13.

#### Dev Seed Data ✅ (checked, n/a)
No `TestDataSeeder` changes — visual-only fix, not data-shape-dependent.

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
