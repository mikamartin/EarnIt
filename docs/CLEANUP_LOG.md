# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 39 — `refactor/split-settings-screen` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s `SettingsScreen.kt` item (~430 lines in one composable) — same pattern as the `RewardDetailScreen`/`TaskEditScreen` splits: pull screen-only pieces into `private` composables in the same file, no new files/folders, no behavior change.

#### Duplication ✅ (1 reviewed, not fixed)
- `SettingsAppearanceSection`, `SettingsRewardsSection`, and `SettingsTasksSection` each still hand-roll the same section-header `Row` (bold label + optional `InfoIconButton`) instead of extending the existing `SettingsSectionHeader` helper (used as-is by About/Data/Clean Up, which have no trailing icon) with an optional trailing-content slot. Pre-existing duplication, not introduced by this split — left as-is since fixing it changes `SettingsSectionHeader`'s signature, beyond a pure structural split. Worth a follow-up if this file is touched again.

#### Decoupling ✅ (1 fix)
- **Fixed:** `showRewardsInfo`/`optimalText`/`maxText` (Rewards) and `showTasksInfo` (Tasks) were hoisted to the top of `SettingsScreen` even though nothing outside their own section ever read them — pure prop-drilling with no purpose. Moved into `SettingsRewardsSection`/`SettingsTasksSection` themselves. Only `showMascotPicker`/`highlightedMascot` stay hoisted at the orchestrator, since they're genuinely driven from outside (the `viewModel.openMascotPicker` `LaunchedEffect`, fired when a claim unlocks a new mascot).

#### Complexity & Pattern Health ✅
- `SettingsScreen` cut from ~430 lines to a 58-line orchestrator (state + one `LaunchedEffect` + six section calls) plus 8 private helpers: `SettingsAboutSection`, `SettingsAppearanceSection` (which itself calls `SettingsNicknameCard`/`SettingsThemeCard`/`SettingsMascotAndQuoteCard`), `SettingsRewardsSection`, `SettingsTasksSection`, `SettingsDataSection`, `SettingsCleanUpSection`.
- `SettingsNicknameCard`/`SettingsThemeCard`/`SettingsMascotAndQuoteCard` each have exactly one caller (`SettingsAppearanceSection`) — reviewed against "is this extraction earning its keep": yes, this is the same card-per-composable pattern the backlog itself specified, and matches `RewardDetailScreen`'s single-caller helpers from its own split.

#### Dead Code & Hygiene ✅
- No unused imports (ktlint's unused-import rule ran clean on every check).
- `git status` clean apart from the intended 4 files (`SettingsScreen.kt`, `SettingsScreenUiTest.kt`, `TESTING.md`, `CLEANUP_BACKLOG.md`).

#### Naming Consistency ✅
- New composables follow the existing `Settings*` naming already established by `SettingsCard`/`SettingsSectionHeader`/`ThemeChip`/`MascotPickerDialog`/`DangerButton` in this file.

#### Hardcoded Values ✅
- None touched.

#### Accessibility ✅ (1 fix)
- **Fixed:** the "Show daily quote" `Switch` had no `contentDescription` — every other toggle on this screen (Notes Required) already does. Added `contentDescription = Strings.SETTINGS_QUOTE_TOGGLE`, reusing the existing label string, matching the Notes Required precedent exactly. Needed to make the toggle reliably targetable in the new instrumented test, but it's a genuine pre-existing accessibility gap independent of that.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (2 findings — 1 fixed, 1 flagged not actioned)
- **Fixed:** the Max Reward Count row said only "Hard cap; banner shown when exceeded," which undersold what's actually built and confirmed intended: the FAB dims to 40% opacity at the cap but stays tappable (not a disabled button) — tapping it shows a 2s tooltip instead of navigating, and a separate 3s banner auto-appears the moment a new reward reaches the cap. Row expanded to describe both surfaces and the FAB's non-disabled tappable state.
- **Found while writing spec-grounded tests, not actioned:** `optimalRewardCount` is read back from DataStore and displayed in its own edit field, but is never consumed anywhere else in the app — no banner, no home-screen guidance, nothing reads it besides the Settings field itself. The spec table describes it as "Soft limit; shown as guidance," which isn't true today. Pre-existing (not introduced by this branch), out of scope for a structural-split pass to fix since closing it is a product decision (what should "shown as guidance" look like?), not a mechanical fix — flagged to the user rather than silently patched or silently left in the spec.

#### Tests ✅ (10 new tests, spec-grounded)
- Reviewed instrumented coverage of `SettingsScreen` before writing anything: `SettingsUiTest` (colour scheme persistence, Notes Required) and `SettingsTipUiTest` (discoverability tip) were the only coverage; nickname, random nickname, show-quote, mascot picker, reward-count fields via the UI itself, and the About/Data/Clean Up nav rows had none.
- Added `SettingsScreenUiTest` (10 tests, new file), each grounded in an `EARNIT_SPEC.md` §6 line rather than mirroring implementation: nickname typed in Settings shows in the home greeting; clearing it shows "Earn It!" with no address (the exact spec wording); enabling random nickname overrides the typed name on the greeting ("chosen each session instead of the saved name"); Show Quote toggle hides/shows the daily quote section on Home; Max Reward Count edited through the Settings field itself (not the repository, closing the gap `RewardLimitUiTest` left) still enforces the FAB's max-limit tooltip; the mascot picker's default unlocked set is exactly Pugsly and Tabby per the spec table, with the next-locked mascot's unlock hint shown and further-locked mascots showing neither name nor hint; selecting an unlocked mascot persists after `activityRule.scenario.recreate()`; About/Data & Backup/Clean Up rows navigate to their respective screens.
- One test initially failed on-device (`mascotPicker_defaultUnlockedSet...`): `onNodeWithText("Pugsly").assertIsDisplayed()` found 2 nodes, not 1 — the mascot-picker dialog doesn't unmount the row behind it, and that row already shows "Pugsly" as the current-mascot label. Fixed by asserting via `onAllNodesWithText(...).isNotEmpty()` instead of requiring a single unique match.
- Full instrumented suite run on a connected emulator, not just compiled: 73/73 pass (was 63 before this branch).
- `TESTING.md` updated: `SettingsScreenUiTest` row added; UI test pyramid count `~30` → `~40` (rounded).
- `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest`, `assembleDebug`, and `connectedDebugAndroidTest` (full suite) all pass, run sequentially per `CLAUDE.md`.

---

### Pass 40 — `chore/remove-optimal-reward-count` branch, merged via PR #29 (backfill entry)

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s `optimalRewardCount` item, flagged during Pass 39. This entry is written after the fact — the branch shipped without a matching log entry, caught while auditing the backlog for the next split branch (Pass 41).

#### Dead Code & Hygiene ✅ (1 fix)
- **Fixed:** removed `optimalRewardCount` end-to-end — `AppSettings.optimalRewardCount` field, `SettingsRepository`'s `OPTIMAL_REWARD_COUNT` DataStore key + read + `updateOptimalRewardCount`, `EarnItViewModel.updateOptimalRewardCount`, the field and its row in `SettingsScreen`'s `SettingsRewardsSection`, and `Strings.SETTINGS_OPTIMAL_LABEL`. Only `maxRewardCount` remains, matching what's actually consumed by the app.

#### Complexity & Pattern Health ✅ (1 fix)
- `SettingsRewardsSection`'s two side-by-side `OutlinedTextField`s (Optimal + Max) replaced with a single `SliderRow` (range `1..10`, `showValue = true`) bound directly to `maxRewardCount` — reuses the same slider component `TaskEditScreen`'s auto-points fields already use, instead of a bespoke text field. `SliderRow` extended with `range`/`showValue` parameters (previously hardcoded to `1..5`, label-only) to support this.

#### Spec Review ✅ (1 fix)
- `EARNIT_SPEC.md` §6 Settings table: dropped the Optimal Reward Count row; Max Reward Count default corrected `7` → `5` and described as "set via a 1–10 slider in Settings." §10 Screen Map: `Reward Limits` section renamed to `Max Rewards` (matches `Strings.SETTINGS_SECTION_REWARDS`), row text simplified to "max count input."

#### Tests ✅ (1 test count updated, no new file)
- `SettingsScreenUiTest`: existing Max Reward Count test updated to drive the slider instead of the text field, plus a new case asserting the default is `5` on a fresh install. Test count for that file `10` → `11`.
- `TESTING.md` updated: `SettingsScreenUiTest` row test count and description updated; no pyramid-count change (rounds to the same `~40`).

---

### Pass 41 — `refactor/split-reward-edit-screen` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s `RewardEditScreen.kt` item (~420 lines in one composable) — same pattern as the `RewardDetailScreen`/`TaskEditScreen`/`SettingsScreen` splits: pull screen-only pieces into `private` composables in the same file, no new files/folders, no behavior change. Test coverage for this screen was audited up front rather than after the fact, since the `TaskEditScreen` and `SettingsScreen` splits both found real gaps this way.

#### Duplication ✅ (1 reviewed, not fixed)
- `RewardIconAndNameField` and `RewardCostAndDescriptionFields` each still repeat the same `OutlinedTextFieldDefaults.colors(...)` block per field — pre-existing duplication carried over unchanged from the original single composable, not introduced by this split. Same category of finding as Pass 39's `SettingsSectionHeader` note; left as-is since fixing it means introducing a shared colors constant beyond a pure structural split.

#### Decoupling ✅ (reviewed, nothing to hoist)
- Unlike Pass 39's `SettingsScreen` split, no state here was safe to move down into a single section: `name`/`cost`/`description`/`icon` and the three dialog-visibility flags are each read from at least two of {a field section, the dialogs, the bottom-bar save, or a `LaunchedEffect`}. All stay hoisted at the orchestrator, passed down as value+lambda pairs — same shape as `TaskEditScreen`'s own orchestrator state.

#### Complexity & Pattern Health ✅
- `RewardEditScreen` cut from ~420 lines to a ~290-line orchestrator (state + 3 `LaunchedEffect`s + dialogs call + title bar + `LazyColumn` + bottom bar) plus 7 private helpers: `RewardEditDialogs`, `RewardEditTitleBar`, `RewardIconAndNameField`, `RewardCostAndDescriptionFields`, `LazyListScope.rewardEditTasksSection`, `RewardEditTaskRow`, `RewardEditBottomBar`.
- One shape difference from the other three splits: this screen's task list uses `LazyColumn` + `items(includedTasks, key = { it.id })` for real keying, which a plain `@Composable` can't emit. `rewardEditTasksSection` is written as an extension function on `LazyListScope` instead — the standard Compose pattern for extracting list content — so the emitted items and keys are unchanged from before the split.
- `RewardEditTaskRow` has exactly one caller (`rewardEditTasksSection`'s `items{}`) — reviewed against "is this extraction earning its keep": yes, it isolates the per-row Card content the same way `RewardDetailScreen`'s single-caller helpers did in its own split.

#### Dead Code & Hygiene ✅
- No unused imports (ktlint's unused-import rule ran clean on every check).
- `git status` clean apart from the intended files: `RewardEditScreen.kt`, `RewardEditScreenUiTest.kt` (new), `Strings.kt`, `RewardDetailScreen.kt`, `TESTING.md`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md` (the test-coverage follow-up below didn't add any new files).

#### Naming Consistency ✅
- New composables follow the existing `Reward*`/`TaskEdit*` naming convention already established by the `TaskEditScreen` split (`RewardEditDialogs` ↔ `TaskEditDialogs`, `RewardEditTitleBar` ↔ `TaskEditTitleBar`, etc.).

#### Hardcoded Values ✅
- None touched.

#### Accessibility ✅ (2 fixes)
- **Fixed:** the included-task row's `Checkbox` had no `contentDescription` — the only way to un-include a task was a bare checkbox with no accessible label. Added `Strings.REWARD_INCLUDED_DESC` ("Included"), needed to make the checkbox targetable in the new instrumented test, but also a genuine pre-existing accessibility gap independent of that (same category as Pass 39's `SETTINGS_QUOTE_TOGGLE` fix).
- **Fixed (drive-by, different file):** `RewardDetailScreen`'s edit button (`RewardHeaderCard`) is the *only* way to reach Reward Edit for an existing reward, and it had `contentDescription = null` on its `Icon` with no label anywhere on the clickable `Box` — the exact same gap `TaskDetailScreen`'s equivalent edit button already had fixed (`Strings.EDIT_TASK_DESC`). Added the missing `Strings.EDIT_REWARD_DESC` via `.semantics { contentDescription = ... }` on the `Box`, mirroring `TaskDetailScreen.kt`'s pattern exactly. Required to reach the existing-reward edit/pre-population tests at all; flagged here since it's outside `RewardEditScreen.kt` itself.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (no changes needed)
- `EARNIT_SPEC.md` §10 Screen Map's "Reward Edit" row was checked against current behavior — name, icon, description, cost, linked tasks, duplicate-name handling, "Add task" gating, and post-save navigation are all still accurately described. No drift, since the split is behavior-preserving.

#### Tests ✅ (10 new tests, real gap closed, one real bug found)
- Coverage audited before writing anything, not after: grepped `app/src/androidTest` for `RewardEdit`/`saveReward`/`deleteReward` — `RewardEditScreen` had no dedicated UI test file. Only incidental coverage existed: `SaveNavigationUiTest` (post-save nav, create-task-from-reward-form), `MaxLengthUiTest` (name/description char caps), `DuplicateNameUiTest` (duplicate reward name). Untested: icon picker, cost field's digit filter, the included-task row's mandatory/repeatable toggles and uncheck-to-remove, editing-and-persisting an existing reward, and task-link pre-population on reopen — the same shape of gap the `TaskEditScreen` split found and closed on the task side.
- First pass added 6 tests covering the above. Reviewing that pass against what it actually exercised (prompted by a direct question about it, not a self-review) surfaced three more gaps worth closing rather than leaving for a manual-only check: no test used more than one task in the included list at once (so nothing proved per-row toggle isolation), no test drove `AddTaskToRewardDialog`'s *own* checkbox-selection path (only the "create new" shortcut), and the "Browse Library" entry point from that dialog was untested from every call site in the app, not just this one.
- Investigated whether those three were genuinely worth automating or reasonably deferred like the dialog's collapse/expand/select-all UI state already is (see `TESTING.md`'s `AddTaskToRewardDialog` deferral note): row isolation and the checkbox-selection path both feed real state into what eventually gets saved, so they're closed; Cancel/dismiss buttons (checked broadly — untested on *every* screen and dialog in the app, not specific to this one) and the dialog's internal collapse/expand/select-all/search-filter state stay deferred, logged as their own `CLEANUP_BACKLOG.md` items rather than silently dropped.
- Added 4 more tests to `RewardEditScreenUiTest` (10 total): `taskRows_multipleTasksToggleAndRemoveIndependently` (two tasks added via the dialog's checkbox list in one session toggle/remove independently — each assertion scoped to its own row via `onSiblings()` + `filterToOne(hasContentDescription(...))` rather than a bare `onNodeWithContentDescription()`, since `REWARD_MANDATORY_DESC`/`REWARD_OPTIONAL_DESC`/etc. aren't unique per row and the `LazyColumn`'s viewport doesn't reliably keep both rows composed at once on a small screen); `existingTaskSelection_viaDialogCarriesMandatoryFlagThroughToIncludedList` (selecting an existing task via the dialog's checkbox and toggling its mandatory flag inline, before confirming, carries that flag through to the included list); `addTaskDialog_browseLibraryNavigatesToTaskLibrary` (smoke test for the navigation call this split's own extraction touched); and one bug-documenting test (below).
- **Found a real, pre-existing bug while writing the row-isolation test**, not a test artifact: adding two new tasks in a row via "Create your own" to an *unsaved* reward silently drops the first task's inclusion. Root cause: `taskState`/`taskStateReady` are plain `remember`, and Navigation Compose disposes+recreates `RewardEditScreen`'s composition on every round-trip to `TaskEditScreen` and back, resetting both; the startup effect then re-derives every task's inclusion from the still-unsaved reward (which has none), and only the single most recent `pendingTaskId` gets explicitly restored. Confirmed pre-existing, not introduced by this split, by reading the `LaunchedEffect` logic (copied verbatim, unchanged). Decision (discussed with the user): defer the actual fix — out of scope for a structural-split branch — but don't leave it silently uncovered. Rewrote the original isolation test to add both tasks via the dialog's checkbox list instead (a path that doesn't hit the bug, since it never leaves the screen), and added `sequentialCreateNewTasks_onUnsavedReward_onlyLastOneStaysIncluded` as a dedicated regression test pinning today's actual (buggy) behavior — a red test for a future fix to turn green, not a silent gap. Logged as its own `CLEANUP_BACKLOG.md` Correctness item with root cause, current status, and a workaround for users today.
- Two rounds of on-device iteration to make the row-isolation test itself reliable, both logged so the reasoning isn't lost: (1) chaining "create task → assert" for two tasks back-to-back without a wait in between raced the async `pendingTaskId` propagation — the same class of flake Pass 37/38 already hit and fixed for a different test — fixed by asserting after each creation before starting the next; (2) `Strings.REWARD_ADD_TASK_BTN` and `Strings.ADD_TASK_DIALOG_TITLE` are both literally `"Add task"`, so a bare `onNodeWithText` match on the dialog title was ambiguous against the still-present button behind it — removed that assertion rather than special-case a match.
- One test initially timed out on-device (`editExistingReward_taskLinksPrepopulateFromExistingLinks`): the shared `waitForRewardDetail()` helper waits for the empty-state copy (`REWARD_DETAIL_NO_TASKS`), but this test's reward has a task, so that text never appears. Fixed by waiting for the task's own name instead.
- Full instrumented suite run on a connected emulator twice, not just compiled: first run (6-test version) 79/80 pass (was 74 before this branch — 73 as of Pass 39, +1 from Pass 40's `SettingsScreenUiTest` addition), with the one failure (`taskRow_mandatoryRepeatableTogglesAndUncheckRemoves`, teardown-only: `ActivityScenario.close()`: "Activity never becomes requested state [DESTROYED]", after its own assertions had already passed) confirmed as an emulator-load flake by re-running in isolation twice, both clean — not the same root cause as Pass 37/38's cross-test data leakage, since this test's `@Before` already runs `resetAppState()`. `RewardEditScreenUiTest` alone (10-test version) re-ran clean, 10/10.
- `TESTING.md` updated: `RewardEditScreenUiTest` row description and count (6 → 10); UI test pyramid count `~40` → `~50`; instrumented-suite header count `~75` → `~85` (both rounded); the `AddTaskToRewardDialog` deferral note narrowed to what's actually still deferred (collapse/expand, select-all, search filter) now that the selection mechanism itself is covered.
- `./gradlew ktlintCheck`, `assembleDebug`, `test`, `assembleDebugAndroidTest`, and `connectedDebugAndroidTest` (full suite, 84 tests, targeted at the emulator to avoid the known Android 16 physical-device gap — see `TESTING.md` Deferrals) all pass, run sequentially per `CLAUDE.md`. Full-suite re-run after the final 4-test addition: 84/84 clean, in 5m52s — no flakes this time.
