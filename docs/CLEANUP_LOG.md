# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

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

---

### Pass 70 — `test/settings-persistence-and-assertions` branch

QA audit Issues 6 and 7: two `SettingsScreenUiTest` `...persistsAfterRecreate` tests only re-read the DataStore-backed repository after `activityRule.scenario.recreate()`, which was already written before the recreate — they'd pass identically with the `recreate()` line deleted. `useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting` only asserted the *old* nickname was gone, never that a random one actually appeared, and `mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable` matched text anywhere on screen instead of scoping to the dialog. While fixing these, found that the audit's own cited counter-example, `SettingsUiTest.colorScheme_selectionPersistsAfterRecreate`, had the identical repo-only bug — not on the backlog list, fixed anyway for consistency, confirmed with the user first since it needed a small production change (see Accessibility below).

#### Duplication ✅ (checked, n/a)
The "re-navigate to Settings and assert rendered state" snippet now appears in three tests (colorScheme, mascot, cloud backup); each is 1–3 lines with a different target node, not worth extracting into a shared helper at this size.

#### Decoupling ✅ (checked, n/a)
Test-only changes plus one one-line production modifier swap in `ThemeChip` (`.clickable` → `.selectable`) — no logic moved between layers.

#### Complexity & Pattern Health ✅ (checked)
`Modifier.selectable(selected =, onClick =)` is the standard Compose replacement for a manually-tracked `selected` boolean plus `.clickable` — not a custom reimplementation of M3 selection behavior. `ThemeChip` is unchanged in size and structure.

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` clean. Confirmed `clickable` (the import) is still used elsewhere in `SettingsScreen.kt` before leaving it in place. `git status` shows exactly the 5 intended files changed.

#### Naming Consistency ✅ (checked, n/a)

#### Hardcoded Values ✅ (checked, n/a)

#### Accessibility ✅ — found and fixed
`ThemeChip`'s selected state was previously conveyed only through border color/width and font weight — invisible to the semantics tree, so TalkBack couldn't announce which color scheme was selected. Exposing it via `Modifier.selectable`'s standard `selected` semantics fixes that alongside making the state testable; confirmed with the user before making the production change since it was outside the branch's original test-only scope.

#### Deprecated APIs ✅ (checked, n/a)

#### Spec Review ✅ (checked, n/a)
No behavior change — `EARNIT_SPEC.md` doesn't document test/UI semantics at this level of detail.

#### Tests ✅ — this branch's entire purpose
Rewrote `selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate` and `cloudBackupToggle_defaultsOn_turnedOff_persistsAfterRecreate` to assert against rendered UI after recreate (Settings mascot row text; cloud-backup Switch `assertIsOff()`), keeping the repository check as a secondary assertion. Fixed `colorScheme_selectionPersistsAfterRecreate` the same way via the new `selected` semantics. Made `useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting` assert a random-nickname-shaped greeting actually renders (`SemanticsMatcher` on text starting with `"Earn It, "` and not equal to the old value) and dropped its one redundant `assertEquals`. Scoped `mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable`'s Pugsly/Tabby lookups to the dialog via `hasAnyAncestor(isDialog())` (mirroring `CancelDismissAssertions`), asserted both carry a click action, and added a check that Panda's name is hidden.
Load-bearing check: temporarily reverted the `ThemeChip` `.selectable(...)` change and re-ran `SettingsUiTest` — `colorScheme_selectionPersistsAfterRecreate` failed with a real `AssertionError` on `assertIsSelected()`, confirming the new assertion isn't vacuous; reverted the revert and re-confirmed green.
Ran `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.earnit.app.SettingsScreenUiTest,com.earnit.app.SettingsUiTest` on the connected emulator (API 36): 16/16 passed, 0 skipped, 0 failed.
No `AppModule`/`TestAppModule` change or new `@Inject` site — ran `./gradlew assembleDebugAndroidTest` anyway per the Hilt-graph-sanity-check convention; passed.
`checkInstrumentedTestTags`: both files already carry their required layer tags; no new test classes added, no tag changes needed.
`TESTING.md`: `SettingsUiTest` and `SettingsScreenUiTest` row descriptions updated to state the recreate tests now confirm rendered UI, the nickname test asserts a positive outcome, and the mascot-picker checks are dialog-scoped. No aggregate counts changed — same number of tests, no new files.
`./gradlew ktlintCheck`, `test` (204/204), `assembleDebugAndroidTest` all pass sequentially.

#### Dev Seed Data ✅ (checked, n/a)
No `TestDataSeeder` changes — this branch touches only DataStore-backed settings already covered by existing seed data.
