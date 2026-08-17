# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

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

---

### Pass 69 — `chore/release-v1.3.0-prep` branch (doc sync ahead of the v1.3.0 release cut)

User asked to confirm README.md and EARNIT_SPEC.md were current before cutting the v1.3.0 release. Pass 66–68 had each done a spec review, so EARNIT_SPEC.md's feature coverage was accurate, but README.md was never touched by any of the three preceding feature branches and had drifted; both docs also carried one shared stale claim and a stale test-count line.

Most checklist sections are n/a — this is a documentation-only pass, no source changes.

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility / Deprecated APIs / Dev Seed Data — n/a
No source code touched this pass.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only the intended doc + `app/build.gradle.kts` changes.

#### Spec Review ✅ — found and fixed
- `README.md`: missing the first-launch onboarding tutorial from the feature list entirely; the backup bullet didn't mention it's opt-out; the intro line and Tech Stack table both claimed an absolute "local only, no cloud sync" that contradicts the Auto Backup opt-out shipped in Pass 67 (the same class of inaccuracy Pass 67 already fixed in the in-app About copy, just missed in README). Fixed all three.
- `EARNIT_SPEC.md`: the Overview and Stack table carried the same absolute "local only, no cloud" wording as README, inconsistent with §7's own accurate description of the opt-out backup. Fixed both to match §7, with a cross-reference link from the Overview.
- `EARNIT_SPEC.md`'s test-summary line (§ Tests) was stale on two axes: test counts (203/119/85 vs. actual 206/120/85 per `TESTING.md`'s current headers) and file counts (claimed 29/41, actual 28/32 — verified against both the filesystem and `TESTING.md`'s own table row counts). Corrected to 206 unit tests / 28 files, 120 instrumented / 32 files, 85 Compose UI tests.
- Did not touch `TESTING.md`'s test pyramid (`Unit — 205 tests` / `UI — 85 tests` / `Integration — 34 tests`, which sums to 119 against the section header's 120) — that's a pre-existing internal inconsistency predating this branch, out of scope for a doc-sync pass; flagging here rather than silently leaving it for the next person to rediscover.

#### Tests — n/a
No test files changed.

#### Release
Version bump for the v1.3.0 cut: `app/build.gradle.kts` `versionCode` 3→4, `versionName` "1.2.0"→"1.3.0", per `DEV_PLAYBOOK.md`'s release process.
