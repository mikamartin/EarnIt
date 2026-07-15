# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

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

---

### Pass 42 — `refactor/split-home-screen` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s `HomeScreen.kt` item (~320 lines in one composable) — same pattern as the `RewardDetailScreen`/`TaskEditScreen`/`SettingsScreen`/`RewardEditScreen` splits: pull screen-only pieces into `private` composables in the same file, no new files/folders. One deliberate behavior change was made alongside the split (confirmed with the user, not silently bundled): the bottom slide-up "max reward banner" (`showMaxBanner`, its `LaunchedEffect`, and its UI) was removed entirely, since it duplicated the FAB tooltip's message in a second on-screen location. The FAB tooltip itself was kept, with `Strings.MAX_REWARD_BANNER` renamed to `MAX_REWARD_TOOLTIP` to match what it now actually is.

#### Duplication ✅ (reviewed, nothing new)
- No styling/gradient patterns repeated inline beyond what already existed pre-split; the extracted helpers carry code forward verbatim.

#### Decoupling ✅ (reviewed, nothing to change)
- `HomeDialogs` takes the full `EarnItViewModel` — checked whether this could be narrowed to specific lambdas, but `LogTaskDialog`/`ClaimDialog` (`SharedDialogs.kt`) already require the full `viewModel` in their own public signature and are shared with `RewardDetailScreen`, so narrowing would mean changing a shared dialog's API, out of scope for a structural-split branch.
- `homeRewardListItems` (a `LazyListScope` extension, same shape as Pass 41's `rewardEditTasksSection`) takes `isDragging`/`draggingIndex`/`draggingRewardId` as individual value+callback pairs rather than bundled state. Confirmed via grep that `draggingIndex`/`draggingRewardId` are read/written only inside this function, while `isDragging` is also read by the orchestrator's list-sync `LaunchedEffect` — so at least `isDragging` must stay hoisted. `LazyListScope`'s content lambda isn't itself a composable context, so `remember` can't live inside `homeRewardListItems` directly; hoisting is required, not avoidable. Bundling the drag-index/id pair into one hoisted value would trim the 15-parameter signature but changes state shape, not just relocates it — left as a future opportunity if this file is touched again, not actioned here.

#### Complexity & Pattern Health ✅
- `HomeScreen(...)` cut from ~320 lines to a ~131-line orchestrator (state + 3 `LaunchedEffect`s + dialogs call + `Column { HomeHeader, LazyColumn, HomeAddRewardFab }`) plus 4 private helpers: `HomeHeader`, `HomeDialogs`, `BoxScope.HomeAddRewardFab`, `LazyListScope.homeRewardListItems`.
- Confirmed via grep that each of the 4 new helpers has exactly one call site — reviewed against "is this extraction earning its keep": yes, consistent with the single-caller pattern already accepted in Pass 39/41.

#### Dead Code & Hygiene ✅ (1 fix)
- **Fixed:** a stale comment in `RewardLimitUiTest.kt` still said "MAX_REWARD_BANNER text becomes visible" after the `Strings` constant was renamed to `MAX_REWARD_TOOLTIP`.
- No unused imports (ktlint clean on every check, re-verified after all edits).
- Confirmed via diff that no new `Color(0xFF...)` literals were introduced.
- `git status` clean apart from the intended files: `HomeScreen.kt`, `RewardLimitUiTest.kt`, `SettingsScreenUiTest.kt`, `Strings.kt`, `EARNIT_SPEC.md`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅
- New composables follow the existing `Home*`/`*Dialogs` naming already established (`HomeDialogs` ↔ `RewardEditDialogs`/`TaskEditDialogs`).

#### Hardcoded Values ✅
- None introduced; existing values (16.dp, 56.dp, etc.) carried over unchanged.

#### Accessibility ✅ (reviewed, nothing new)
- Confirmed via `git show HEAD` that the mascot `Image`'s `contentDescription = null` predates this branch — not introduced or touched by the split.
- The FAB's `Icon` keeps its existing `contentDescription = Strings.NEW_REWARD_DESC`, unchanged.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (1 fix)
- **Fixed:** `EARNIT_SPEC.md` §6 Settings table's Max Reward Count row still described "A separate 3s banner also auto-appears the moment a new reward reaches the cap" (added in Pass 39) — that banner was removed as part of this split, so the row was trimmed to describe only the tooltip behavior that remains.
- Checked §10 Screen Map's "Prizes (Home)" row against current behavior (progress cards, mascot, quote of the day) — still accurate, no further drift.

#### Tests ✅ (1 fix, 1 new gap logged, 1 unrelated gap found and logged)
- Coverage reviewed for the area before finalizing: FAB→RewardEdit navigation under the cap (`SaveNavigationUiTest`), FAB tooltip at the cap (`RewardLimitUiTest`, `SettingsScreenUiTest`), empty state (`EmptyStateUiTest`), show-quote toggle (`SettingsScreenUiTest`), card tap/log/claim dialogs (`UiHappyPathTest`), and reorder persistence (`SortOrderTest`, unit) are all still exercised and unaffected by the split.
- **Gap found and logged, not fixed here:** the drag-to-reorder gesture itself (relocated into `homeRewardListItems`) has no automated coverage at any level and isn't mentioned in `TESTING.md`'s Tier 4 or "Not Covered" sections — pre-existing, not introduced by this split. Logged as a new `CLEANUP_BACKLOG.md` Test Coverage item rather than left unmentioned.
- No test ever exercised the removed bottom banner's own behavior (its 3s auto-show/hide timing), so nothing needed to be deleted for its removal.
- `TESTING.md` required no count updates — no tests added/removed by this pass, and its existing `RewardLimitUiTest`/`SettingsScreenUiTest` rows already described only the tooltip.
- **Found, unrelated to this branch's scope:** `RewardDetailScreen.kt` (not touched by this split) renders its current-points number in dark, muted `onSurfaceVariant` text below ~12% progress instead of the white it uses once the fill takes over, making low-progress rewards look inconsistent with further-along ones — noticed during the user's manual pass on unrelated test data. Logged as a new `CLEANUP_BACKLOG.md` Visual Polish item since it's pre-existing and outside this branch.
- `./gradlew ktlintCheck`, `assembleDebug`, `test`, and `connectedDebugAndroidTest` (full suite, 84/84) all pass, run sequentially per `CLAUDE.md`.
