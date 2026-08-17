# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 66 — `feature/onboarding-tutorial` branch

Ran against the whole branch: the two original feature commits (`4fc7244` feat, `0dbe1f9` test) plus this session's UX/copy pass and bug fixes on top, reviewed together as one diff against `main`.

#### Duplication ✅ (checked)
`RewardIconAndNameField`/`TaskIconAndNameField` are near-identical (icon button + name field + conditional duplicate-name error) — but that predates this branch (present on `main` before onboarding work started). This session touched both identically to fix the layout bug below, without adding new duplication. Considered extracting a shared composable; didn't — the two differ in max-char constant, default icon glyph, label, and error-string function, enough that a shared version would need nearly as many parameters as either version's own body saves. No inline strings bypassing `Strings.kt`, no hardcoded colors.

#### Decoupling ✅ (checked, n/a)
No new coupling introduced. `OnboardingStep` living in the same file as Compose UI code, and the ViewModel using `mutableStateOf` directly for `onboardingStep`/`sessionNickname`, are both pre-existing patterns from the original feature commits — unchanged this session.

#### Complexity & Pattern Health ✅ — found and fixed two real bugs
1. **Layout-overlap bug.** `TaskPointsSection`, `TaskIconAndNameField`, and `RewardIconAndNameField` each emit more than one top-level composable (a toggle row + a conditional sliders card + a totals row; or a row + a conditional error text). Wrapping a call like that in a bare `Box` (for onboarding anchor-capture) breaks Column-based vertical stacking — `Box` overlays multiple children instead of stacking them, which is exactly why "Total points" rendered on top of "Time" once auto-points was on, and would have identically hidden the duplicate-name error text behind the name field. Fixed by giving each composable its own `modifier` parameter and a single root `Column`, called with `modifier = Modifier.captureOnboardingAnchor { }` directly instead of an external `Box` wrapper.
2. `taskCoachStep` used raw `Int` literals (`0`/`1`) for a two-state sub-flow, inconsistent with the rest of this same feature's typed state (`OnboardingStep`/`OnboardingField` sealed types/enums). Renamed to a `Boolean` (`taskCoachedOnPoints`).

Also noted, not fixed: `TaskEditScreen` (196 lines already on `main`, before this branch) and `RewardEditScreen` (252 lines already after the original onboarding commits, before this session) both exceed the ~150-line composable guideline. This session added ~49 lines to `TaskEditScreen` for the coaching bubble and a net +1 line to `RewardEditScreen`. Considered extracting the coaching scrim+bubble into a helper composable; not done — the scrim needs to render inside the scrollable `Box` (to dim just that section) while the bubble renders outside it in the same `Column`, so there's no single natural call site to extract to without restructuring the box hierarchy.

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` passes. `git status` shows exactly the 8 intended files changed, no stray untracked files. No TODO/FIXME introduced, no commented-out code.

#### Naming Consistency ✅ (checked)
New `Strings.kt` constants (`ONBOARDING_TASK_LINKED_LINE`, `ONBOARDING_TASK_NAME_LINE`, `ONBOARDING_TASK_POINTS_LINE`, `ONBOARDING_SAVE_BLOCKED_LINE`, `ONBOARDING_STARTER_CHIPS_LABEL`) follow the existing `ONBOARDING_*` convention. No new files this session.

#### Hardcoded Values ✅ (checked)
No new hex colors. The `taskCoachStep` magic-number fix is covered under Complexity above.

#### Accessibility / Deprecated APIs ✅ (n/a)
No new icon-only buttons or tappable targets, no deprecation warnings from this session's changes.

#### Spec Review ✅ — found and fixed a significant gap
`EARNIT_SPEC.md` didn't mention the onboarding tutorial at all — a full first-launch feature, entirely undocumented. Added §3a ("First-Launch Onboarding Tutorial") describing the flow, the task-creation detour, and the save-blocked state; added an "Onboarding Seen" row to the App Settings table cross-referencing it.

#### Tests ✅ — found and closed real coverage gaps
- The entire two-step task-creation coaching sub-flow (name → points), the "task linked" copy switch, and the "Save blocked" fallback had zero coverage despite being added mid-session. Extended `OnboardingFlowUiTest`'s happy-path test to assert all three; added `replayTutorial_duplicateRewardName_blocksSaveWithExplanation`, which seeds a reward whose name matches a starter chip, replays the tutorial, picks that same chip, and confirms Save is disabled with the explanatory message shown instead of a silently-dead button.
- Ran the extended and new tests on a connected emulator. A second physical device joined the adb pool mid-session, causing an unrelated `IllegalStateException: No compose hierarchies found in the app` on `connectedDebugAndroidTest` (it runs across every attached device by default); confirmed as environment noise rather than a regression by re-running pinned to the emulator alone (`ANDROID_SERIAL=emulator-5554`) — 0 failed, both before and after the `taskCoachedOnPoints` rename.
- `TESTING.md`: added `OnboardingStepTest` (11) and `OnboardingFlowUiTest` (3, up from 2) rows. While updating, found the doc's file counts (26 unit / 33 instrumented files) were already stale on `main` itself, unrelated to this branch — actual was 28 / 40 — corrected to current true figures (29 / 41) instead of adding this branch's delta on top of a wrong baseline. Aggregate test counts rounded per the doc's own stated convention (ballpark, not a maintained tally): Unit 189/192 (the pyramid and section header disagreed with each other before this pass) → 205; Instrumented 116 → 120; UI 80/82 → 85.
- `EARNIT_SPEC.md`'s own test-summary line updated to match, kept exact per that line's existing convention: 203 unit / 119 instrumented / 85 UI.
- `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest` all pass. `connectedDebugAndroidTest` against `OnboardingFlowUiTest` (pinned to the emulator) — 0 failed.

#### Dev Seed Data ✅ (checked, n/a)
Onboarding is specifically a first-launch/clean-state flow — it doesn't read or depend on `TestDataSeeder`'s dev-mode seed data.

---

### Pass 67 — `fix/backup-privacy-opt-out` branch

`allowBackup="true"` plus `data_extraction_rules.xml` meant Android's Auto Backup silently uploaded the Room database and DataStore preferences to the user's Google account, contradicting the About screen's "all your data stays right here on your phone" claim (confirmed by reproducing it: wipe → uninstall → reinstall → old data came back). Added an opt-out toggle (`SettingsRepository.cloudBackupEnabled`, default `true`, so existing users see no behavior change), enforced via a new `EarnItBackupAgent.onFullBackup` override — `allowBackup`/`dataExtractionRules` are static manifest attributes the OS reads once at install time, so they can't be toggled at runtime. Corrected the About screen copy to describe the opt-out instead of claiming an absolute "never leaves the phone."

This checklist was first run mid-branch, before instrumented tests existed, with the Tests section explicitly flagged as deferred pending the user's manual pass. The user asked for the tests to be written immediately instead of waiting; the Tests section below reflects that they're now written and passing, not the original deferred state.

#### Duplication ✅ (checked)
The new toggle reuses the existing `SettingsCard` + `Switch` + `Modifier.semantics { contentDescription = ... }` pattern already used for `SETTINGS_QUOTE_TOGGLE`/`SETTINGS_NOTES_TOGGLE` — no new pattern introduced. `updateCloudBackupEnabled` follows the exact single-field `context.dataStore.edit { ... }` shape as every other `SettingsRepository` setter (e.g. `updateShowQuote`). New strings (`DATA_CLOUD_BACKUP_TITLE`/`DATA_CLOUD_BACKUP_SUBTITLE`) live only in `Strings.kt`, no inline duplicates at the call site.

#### Decoupling ✅ (checked, one deliberate exception noted)
`DataScreen.kt`'s new card only reads `settings.cloudBackupEnabled` and calls `viewModel.updateCloudBackupEnabled(it)` — no business logic in the composable. `EarnItBackupAgent` is the one deliberate exception to normal DI: it instantiates `SettingsRepository(applicationContext)` directly rather than via Hilt, because `BackupAgent` is instantiated by the OS via reflection outside the Hilt graph — the same constraint every Android framework-instantiated component in this codebase already works around (documented in the class's own comment and in `EARNIT_SPEC.md` §7).

#### Complexity & Pattern Health ✅ — found and fixed
`DataScreen`'s top-level composable was already ~215 lines (over the ~150-line guideline) before this branch; inlining the new toggle would have pushed it further without adding to an existing extraction. Since the file already extracts `NudgeDebugCard` as its own private composable for exactly this reason, extracted the new toggle the same way as `CloudBackupCard(viewModel, settings)` instead of leaving it inline in `DataScreen`.

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` passes (one indentation violation from the first draft of the About copy string was caught and fixed before this pass). `git status` shows exactly the 7 intended files changed plus the new `backup/` package — no stray untracked files.

#### Naming Consistency ✅ (checked, one judgment call noted)
`cloudBackupEnabled`/`updateCloudBackupEnabled`/`CLOUD_BACKUP_ENABLED` follow the existing Boolean-setting naming convention throughout `SettingsRepository`/`AppSettings`. `EarnItBackupAgent.kt` lives in a new `backup/` package rather than `data/` — CLEANUP_RULES.md's package list (`data/`, `di/`, `ui/`, `viewmodel/`, `widget/`) doesn't name this case explicitly, but it mirrors `widget/`'s existing precedent of a dedicated package for Android-framework-instantiated components (receivers, activities) rather than app-layer classes, so a new sibling package was judged more consistent than forcing it into `data/` alongside repositories it isn't one of.

#### Hardcoded Values ✅ (checked, n/a)
No new colors or magic numbers.

#### Accessibility ✅ (checked)
The new `Switch` carries `Modifier.semantics { contentDescription = Strings.DATA_CLOUD_BACKUP_TITLE }`, matching every other settings toggle's pattern.

#### Deprecated APIs ✅ (checked, n/a)
No deprecation warnings from any new code (`assembleDebug`'s one warning, Moshi kapt codegen, is pre-existing and unrelated).

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md` §7 rewritten to describe the toggle, its default, and why enforcement lives in `EarnItBackupAgent` rather than the manifest. Also found and fixed two related gaps while reviewing: §6's App Settings table was missing a row for the new setting (added, matching the `Onboarding Seen` row's style); the Settings screen tree diagram's "Data & Backup" line still only mentioned export/import (updated to mention the toggle too). Checked Deferred Ideas — no existing entry referenced this work, nothing to remove.

#### Tests ✅ — found and closed the gap
`SettingsRepository` has no unit-test precedent to extend — its DataStore-backed settings are only ever exercised through instrumented tests (e.g. `SettingsUiTest.colorScheme_selectionPersistsAfterRecreate`) — so coverage was added at that layer instead of inventing a new unit-test pattern for one field. Extended `SettingsScreenUiTest.kt` (already the home for the About/Data & Backup nav-row tests this toggle lives alongside) rather than `SettingsUiTest.kt`, since it fit the existing file's stated scope more directly: `cloudBackupToggle_defaultsOn_turnedOff_persistsAfterRecreate` asserts the default is `true` on a fresh install, then that toggling off and recreating the activity persists `false` — same `activityRule.scenario.recreate()` pattern as `selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate`. Added `aboutScreen_doesNotClaimDataNeverLeavesThePhone` as a regression guard for the actual privacy bug: asserts no rendered node contains "stays right here" and that "Google account" is shown — this fails if the old absolute claim is ever reintroduced, not just that *some* text renders.
Ran `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.earnit.app.SettingsScreenUiTest` on the connected emulator (API 36): 14/14 passed, 0 skipped, 0 failed — both new tests confirmed working on a real device, not just compiling.
`checkInstrumentedTestTags`: `SettingsScreenUiTest` already carries its required `@UiTest` layer tag plus `@Settings` as an optional tag — both new tests fit within its existing scope, no tag changes needed.
No `AppModule`/`TestAppModule` change or new `@Inject` site (still just `SettingsRepository`'s existing `Context` constructor) — ran `./gradlew assembleDebugAndroidTest` anyway before running on-device, per the Hilt-graph-sanity-check convention; passed.
`TESTING.md`: `SettingsScreenUiTest` row 12→14 (exact) with both new behaviors described. Pyramid/header aggregate counts recomputed from actual `@Test` counts across `app/src/androidTest/`: UI 85→87 and instrumented total 120→122, both still round to their existing displayed values (85, 120) per the doc's own "nearest 5 or 10, not false precision" convention — left unchanged rather than reprinting the same rounded number.
`./gradlew ktlintCheck`, `test`, `assembleDebug`, `assembleDebugAndroidTest` all pass sequentially.

#### Dev Seed Data ✅ (checked, n/a)
`TestDataSeeder` doesn't reference `SettingsRepository` — it seeds database rows (tasks/rewards/history), not DataStore preferences — so the new toggle needs no seed-data changes.

---

### Pass 68 — Wipe Everything settings reset

User asked whether Wipe Everything left anything behind (theme, dev mode, unlocked mascots, other DataStore settings). It did: `EarnItRepository.clearAll()` only called `database.clearAllTables()` — every `SettingsRepository`-backed preference survived untouched, so "wipe everything" wasn't actually everything. Confirmed the fix direction with the user (`AskUserQuestion`): reset the whole DataStore on Wipe Everything, with one deliberate exception — `cloudBackupEnabled` (the Auto Backup opt-out added in Pass 67) is a privacy choice, not app data, and silently flipping it back to its default-`true` on a "clean slate" wipe would be the opposite of what a privacy toggle should do. Mid-task the user also asked to remove the Settings screen's dismissible discoverability tip banner ("Tip: personalize your name, quote, and color theme below.") — unrelated to the wipe behavior itself, bundled into this branch/PR at the user's request, given as "it isn't really helpful and just adds noise."

#### Duplication ✅ (checked)
`resetForWipeEverything()` is a new `SettingsRepository` method, not a duplicate of `resetToDefaults()` — see Complexity below for why the two need to stay separate rather than being parameterised into one.

#### Decoupling ✅ (checked, n/a)
`EarnItRepository` still knows nothing about settings; the reset is orchestrated from `EarnItViewModel.clearAll()`, which already held both `repository` and `settingsRepository` references for unrelated reasons — no new coupling introduced.

#### Complexity & Pattern Health ✅ — considered and rejected one simplification
Considered making `resetForWipeEverything()` the *only* reset method and having `TestStateReset.resetAppState()` (instrumented test isolation) call it instead of `resetToDefaults()`. Rejected: `SettingsScreenUiTest.cloudBackupToggle_defaultsOn_turnedOff_persistsAfterRecreate` toggles cloud backup off and relies on the *next* test's `resetAppState()` clearing it back to the default `true` — if reset preserved the previous value like the Wipe Everything path does, that would leak `false` into whichever test runs next in the same instrumentation process instead of isolating it. Kept `resetToDefaults()` (full clear, used only by test setup) and `resetForWipeEverything()` (full clear except one key, used only by the real UI action) as two distinct methods rather than one parameterised one, since collapsing them would either break test isolation or need a boolean flag threaded through test-only code for a one-off production concern.

#### Dead Code & Hygiene ✅ — found and removed a full feature, not just its usage
Removed `settingsTipDismissed` (`AppSettings`), `SETTINGS_TIP_DISMISSED` (`SettingsRepository` key + read + `dismissSettingsTip()` setter), `dismissSettingsTip()` (`EarnItViewModel`), `SETTINGS_TIP`/`SETTINGS_TIP_DISMISS_DESC` (`Strings.kt`), the banner call site (`SettingsScreen.kt`), and `SettingsTipUiTest.kt` outright — grepped the repo afterward for every removed symbol name to confirm nothing was left dangling. `DismissibleTipBanner` itself (`EarnItButtons.kt`) is left in place since `RewardDetailScreen`'s widget-linking nudge still uses it. `ktlintCheck` caught one real violation on the first pass — the new `CLEANUP_DIALOG_ALL_BODY` string exceeded the 180-char line limit — fixed by wrapping it as a `+`-concatenated multi-line string, the pattern already used elsewhere in the codebase for long constants.

#### Naming Consistency ✅ (checked)
`resetForWipeEverything()` sits next to `resetToDefaults()`/`resetOnboarding()` and follows their existing `reset*` naming. `WipeEverythingViewModelTest` follows the `<Subject>Test` convention.

#### Hardcoded Values ✅ (checked, n/a)

#### Accessibility ✅ (checked, n/a)
No new tappable elements. The tip banner's dismiss `IconButton` (and its `contentDescription`) was removed along with the whole feature, not left orphaned.

#### Deprecated APIs ✅ (checked, n/a)

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md` §6: removed the "Discoverability Tip" subsection (feature deleted); updated the "Unlocked Mascots" and "Onboarding Seen" table rows to note Wipe Everything as an additional reset path (the former's "never shrinks" claim was no longer accurate without the caveat); added an explicit sentence stating Wipe Everything resets every setting in the table except Cloud Backup Enabled, and why. Updated the App Structure diagram's Clean Up line to mention the settings reset and the backup-choice exception.

#### Tests ✅ — added coverage at both layers, one judgment call noted
- `WipeEverythingViewModelTest` (new file, 1 test): mockk-based, verifies `EarnItViewModel.clearAll()` calls `repository.clearAll()` then `settingsRepository.resetForWipeEverything()` in that order before `onComplete`. This is a single-test file, under `CLEANUP_RULES.md`'s "3+ tests to warrant a new file" guideline — considered folding it into `ImportViewModelErrorTest.kt` (the only existing file with a matching mockk-based `EarnItViewModel` test setup) but kept it separate since that file's own scope (documented by name and in `TESTING.md`) is import error mapping specifically, and `CleanupTest.kt` uses a different base (`RepositoryTestBase`, DAO-level mocks) that doesn't fit a ViewModel-level test. Judged a small clearly-named file clearer than either fit.
- `CleanUpScreenUiTest.kt`: added `clearAllDialog_confirm_wipesDataAndResetsSettingsExceptBackupChoice` (4→5 tests) — the file previously only covered the Cancel path for all four dialogs. Seeds non-default theme/nickname/dev-mode/mascot/backup-toggle values, confirms Wipe Everything, and asserts data is empty, settings are back to default, and the backup choice specifically survived. Hit one real snag writing it: `DangerButton` uppercases its label at render time, so the dialog's confirm button and the card's open button both render the literal text "WIPE EVERYTHING" — `onNodeWithText` can't disambiguate. First fix attempt used `SemanticsNodeInteractionCollection.onLast()`, which doesn't exist in this project's compose-ui-test version — caught by `assembleDebugAndroidTest` failing to compile, not by a passing-for-the-wrong-reason test. Fixed by indexing into `onAllNodesWithText(...)` directly instead.
- Deleted `SettingsTipUiTest.kt` (1 test) rather than leaving it to fail against removed strings — the feature is gone, not just untested.
- Ran `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest` (Hilt/DI graph sanity check — no `AppModule`/`TestAppModule` change, but `SettingsRepository`'s public surface changed), and `assembleDebug` sequentially — all pass. Ran `connectedDebugAndroidTest` pinned to the emulator (`ANDROID_SERIAL=emulator-5554`) for both affected classes: `CleanUpScreenUiTest` 5/5 and `SettingsScreenUiTest` 14/14 (checking the tip banner's removal didn't disturb the rest of the Settings screen) — 0 failed on a real device, not just compiling.
- `TESTING.md`: Unit Tests header 205→206; added the `WipeEverythingViewModelTest` row; `CleanUpScreenUiTest` row 4→5 with the new behavior described; removed the `SettingsTipUiTest` row (Instrumented Tests count stays at 120 — one row removed, one test added elsewhere, net zero); rewrote the "Onboarding nudge dismissal persists" edge case to drop the tip-banner half (now just the widget nudge) and added a new "Wipe Everything resets settings except the backup choice" edge case describing the behavior and its tests.

#### Dev Seed Data ✅ (checked, n/a)
`TestDataSeeder` seeds database rows only; Wipe Everything's settings-reset path and the tip-banner removal don't touch it.
