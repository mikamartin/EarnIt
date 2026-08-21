# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 71 — `chore/qa-audit-doc-fixes` branch

QA audit Issues 8, 9, 10, and 11: dangling `CLEANUP_BACKLOG.md` doc references and stale bug-history comments in `RewardEditScreenUiTest.kt`, `CleanUpScreenUiTest.kt`'s KDoc not covering its fifth (confirm) test, `TestStateReset.kt`'s KDoc describing removed cold-start navigation, a dead `TaskEntity.repeatable` field documented in the spec as functional, drifted test-count figures, and a stale "Widget colors hardcoded warm-gold" Known Limitation. While re-checking README beyond the audit's original scope, also found the opening tagline still carried the same unqualified "local only" claim Pass 69 already fixed elsewhere in the same file.

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility / Deprecated APIs / Dev Seed Data — n/a
Doc/comment fixes plus one one-line Kotlin default-value change; no structural code touched.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only the 10 intended files changed. Removed both `CLEANUP_BACKLOG.md` references (a file that doesn't exist in `docs/`) and rewrote the comments around them to describe current, working behaviour instead of a fixed historical bug.

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md` §1's Task Fields table and §10's Screen Map both documented `TaskEntity.repeatable` as functional (a "repeatable toggle" on Task Edit); confirmed dead in production — `TaskEditScreen.kt` only round-trips the field, nothing reads it, and repeatability is governed entirely by `RewardTaskCrossRef.isRepeatable` instead. Removed both. Kept the column itself rather than deleting it — the app is at Room schema v1 with a hard "every version bump ships a real migration" rule and no existing migration to pattern off, not worth it for a field with zero behavioural payoff. Separately, `RewardTaskCrossRef`'s own constructor default (`isRepeatable = false`) contradicted the documented default (`true`, matching `addTaskToReward`'s own default) — aligned it; confirmed low risk first, since only `GatekeeperTest`/`JsonExportTest` relied on the old default and neither asserts on the value itself.

#### Tests ✅ — counts corrected, no test behaviour changed
The audit's own baseline (204 unit / 121 instrumented) and even `TESTING.md`'s live headers (207/120) were both already stale — three since-merged branches had added tests without updating every count. Recomputed against a fresh `@Test` grep: 220 unit tests / 122 instrumented (87 UI-tagged, 35 Repository/Utility-tagged). Updated `TESTING.md`'s pyramid, headers, and two per-file rows (`FieldValidationTest` 14→20, `RewardProgressTest` 21→28), `EARNIT_SPEC.md` §9, `README.md`'s badge and prose, and `MANUAL_TEST_PLAN.md`'s `ExportImportTest` count (5→11). `./gradlew ktlintCheck`, `test` (220/220), and `assembleDebugAndroidTest` (run specifically because `Entities.kt` changed) all pass sequentially.

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
