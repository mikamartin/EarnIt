# EarnIt — Test Strategy

---

## Risk-Based Approach

Testing is prioritised by what would silently corrupt user data or misrepresent earned points. The app is local-only and single-user, which eliminates concurrency races and network failure modes but makes data integrity the primary risk: there is no server to reconcile against.

**Tier 1 — Core mechanics (highest risk, fully covered)**
The point formula, claim flow, and mandatory-task gating are the app's entire value proposition. A bug here means users earn wrong points or cannot claim rewards they've earned. Covered at unit level with targeted edge cases, then verified end-to-end against a real Room database.

**Tier 2 — Data integrity operations (high risk, fully covered)**
Import/export, clearAll variants, and delete cascades can destroy user data permanently. Each operation has unit-level mock verification (asserting correct DAO calls in correct order) and instrumented tests against a real in-memory database.

**Tier 3 — Settings and state persistence (medium risk, covered)**
DataStore-backed settings that fail to persist degrade every future session. Covered by instrumented UI tests using `activityRule.scenario.recreate()`.

**Tier 4 — UI surface and widget (lower risk, partially deferred)**
Group-view collapse state and dialog checkbox behaviour are pure UI concerns with no data at risk. Bugs are visible and fixed without data recovery. Covered by manual exploratory testing; automated tests deferred on cost/risk grounds. The widget's rendered content (which button shows, text, click wiring) is covered at the JVM level by `WidgetActionButtonTest`/`WidgetContentTest` via `glance-testing`; what's still manual-only is the system-boundary wiring around it — the activity chain, real Room/DataStore data flow, and OS-level rendering — per `MANUAL_TEST_PLAN.md`'s "Widget full flow".

---

## Test Pyramid

```
                 [ Manual — 4 journeys ]   System-boundary flows; see MANUAL_TEST_PLAN.md
            [ UI — 85 tests ]           ComposeTestRule + Hilt, real DataStore
       [ Integration — 34 tests ]       Real in-memory Room, no mocks
     [ Unit — 205 tests ]               JVM, MockK DAOs, fast
```

**Run unit tests** (JVM, no device needed)
```
./gradlew test
```

**Run instrumented tests** (requires connected device or emulator)
```
./gradlew connectedDebugAndroidTest
```

**CI:** Unit tests run on every build (Workflow 1). Instrumented tests run on every push/PR via two parallel API 36 emulator jobs (Workflow 2) — sharded by layer tag, Repository/Utility and UI (see "Tagging convention" below) — and manually before each release candidate. Each job publishes a named Check Run and job summary with pass/fail/skip counts (`mikepenz/action-junit-report`).

---

## Unit Tests — `app/src/test/` (207 tests)

| File | What it covers |
|---|---|
| `PointFormulaTest` (10) | `computeAutoPoints` — min, max, mixed, medium, max single-dimension bonus (time, difficulty, preparation), dimension-5 bonus boundary on `TaskEntity`'s own instance method; `effectivePoints()` auto vs manual override |
| `GatekeeperTest` (7) | `RewardProgress.canClaim` — points boundary (exact / one-below / zero-cost), multiple mandatory tasks (all done / one missing / logged twice), zero-cost with unlogged mandatory |
| `LogAttributionTest` (5) | `logCompletion` — auto-points formula applied, manual points respected, task name snapshotted at log time, `rewardId` + detail recorded, `historyEntryId` null on new log |
| `RepositoryBehaviourTest` (15) | `claimReward` (archives / no-archive / not-found), `saveRewardTasks` (correct flags, clears existing before insert), `copyRewardFromEntry` (flags/icon/description preserved, appended to end of list, skips creating a duplicate when an active reward already has the name, allows it when only an archived reward does, blocked at the active-reward-count cap), `importTemplate` (append / clean-slate / sortOrder / group assignment), `updateTaskRewards` (removes delinked / inserts with correct flags) |
| `ImportDedupTest` (7) | `importTemplate` dedup — exact match, case-insensitive, whitespace-trimmed; non-conflicting tasks inserted; sort order continuous across skips; skipped list preserves template casing |
| `RewardProgressTest` (21) | `totalPoints`, `canClaim` (points gate / mandatory gate / combined / no mandatory), `showsProgressNumbers` (below cost / cost met but mandatory task unlogged / claimable / zero-cost with no mandatory tasks), `progressFraction` (below cap, clamped to 1 above cost, 1 for zero-cost regardless of points), `allTasks` ordering, `isTaskLogged` (match found / no match / distinguishes between tasks with multiple logs present), `loggableTasks` (unlogged / non-repeatable already logged / repeatable re-loggable / mixed set) |
| `ClaimRewardStartOverTest` (3) | `startOver=true` — history entry created, logs archived, reward name/icon/cost snapshotted |
| `DeleteCascadeTest` (2) | `deleteTask` deletes the task; `deleteReward` clears active logs before deleting the reward — cross-ref cleanup for both is handled by the `RewardTaskCrossRef` FK cascade, not a manual repository call, which the test verifies negatively for `deleteReward` |
| `CleanupTest` (3) | `clearAllLogs` deletes all logs (active + archived) **and** all history entries; `clearAllTasks` removes cross-refs then tasks; `clearAllRewards` removes cross-refs + active logs only (history preserved) |
| `WipeEverythingViewModelTest` (1) | `EarnItViewModel.clearAll` calls `repository.clearAll()` then `settingsRepository.resetForWipeEverything()`, in that order, before invoking `onComplete` |
| `SortOrderTest` (7) | `upsertTask` / `upsertReward` sortOrder assignment (empty and non-empty list); upsert routes existing records to update; `updateRewardsSortOrder` / `updateTasksSortOrder` assign sequential indexes |
| `JsonExportTest` (6) | `toJson` / `fromJson` round-trip for tasks, rewards, cross-refs, logs; empty JSON object throws `ImportWrongSchemaException`; `toJson` emits the exact top-level and per-entity key names |
| `JsonImportValidationTest` (10) | `fromJson` error paths — malformed JSON → `InvalidJsonException`; truncated JSON → `InvalidJsonException`; wrong schema → `ImportWrongSchemaException`; null literal → `WrongSchemaException`; JSON array → `WrongSchemaException`; single EarnIt key present → `WrongSchemaException`; foreign object reusing one EarnIt key → `WrongSchemaException`; all five keys present but one holds the wrong element shape → `WrongSchemaException`; valid export round-trips; all-empty export succeeds |
| `ImportViewModelErrorTest` (7) | ViewModel error mapping — each exception type (`FileTooLarge`, `WrongFileType`, `InvalidJson`, `WrongSchema`, `Unreadable`, unknown) calls `onComplete` with the correct string; success calls `onComplete(null)` |
| `MascotUnlockTest` (8) | `Mascots.computeNewlyUnlocked` — each condition type (`ClaimsReached`, `PointsReached`, `TasksCompleted`) unlocks at threshold and not below; already-unlocked mascots not re-returned; multiple thresholds crossed simultaneously returns all |
| `InAppReviewTriggerTest` (2) | `EarnItViewModel.claimReward` — emits `triggerInAppReview` on first claim (empty history); does not emit on subsequent claims |
| `MascotNotificationTest` (3) | `claimReward` sets `hasNewMascot` when a mascot is newly unlocked; does not set it when all already unlocked; `importFromFile` silently seeds unlocked mascots without emitting a notification or setting the badge |
| `PendingRewardIdTest` (3) | `saveReward` sets `pendingRewardId` to the upserted id when creating a new reward; leaves it null when editing an existing reward; `consumePendingRewardId` clears the value |
| `WidgetActionButtonTest` (6) | `widgetActionButtonFor` — no tasks → `ADD_TASK`; unlogged task → `LOG`; repeatable task already logged → still `LOG`; non-repeatable task already logged and below cost → `LOG_DISABLED`; `canClaim` → `CLAIM` even with a loggable task; unlogged mandatory task blocks `CLAIM` despite points met |
| `WidgetContentTest` (12) | Renders `StandardContent`/`FlashContent`/`EmptyState`/`ClaimedState` via `glance-testing` + Robolectric (JVM, no device) — correct button shown/hidden per state with click action wired, disabled non-clickable `LOG_DISABLED` button plus its hint icon (and content description) when all one-time tasks are done, reward name/points/custom-label text, mandatory-hint icon shown/hidden with correct content description, flash and empty/claimed state text |
| `WidgetColorsTest` (4) | `widgetColors()` — `notification` accent matches `ColorSchemes.accents()` for Warm Gold and Forest (red), diverges to amber for Ocean Blue; dark mode changes `primary` from its light-mode value for the same scheme (validates the light/dark branch itself runs), via Robolectric `RuntimeEnvironment.setQualifiers` |
| `NudgeDeciderTest` (10) | `NudgeDecider.decide` — never-logged and no-active-reward guardrails; idle under/at/over the 48h and 96h thresholds; stage 2 never re-sends (two-nudge cap); a new log after stage 1 or stage 2 resets the streak |
| `NudgeWorkerTest` (8) | `NudgeWorker.doWork()` via `androidx.work:work-testing`'s `TestListenableWorkerBuilder` + Robolectric — real notification posted with correct title/body per stage (asserted via `NotificationManager` shadow) and correct `SettingsRepository.updateNudgeState` call for each `NudgeDecider` outcome (first nudge, second nudge, no-op under threshold, no active reward, never logged, stage-2 cap, streak reset), plus the `POST_NOTIFICATIONS`-denied path (state still recorded, no notification shown) |
| `NudgeDebugToolsTest` (3) | `EarnItViewModel.debugGetLastLogIdleHours` — whole-hour idle time from a real timestamp, null when nothing's ever been logged; `debugBackdateLastLog` writes to the repository and invokes its completion callback exactly once (the ordering the "48H"/"96H" dev buttons rely on to avoid racing `NudgeWorker` against an in-flight write) |
| `PugslyGestureTest` (10) | `PugslyGesture.nextState`/`isComplete` — the tap-timing state machine behind the secret mascot gesture: group-gap boundary (exact pass, one ms over resets), pause-window boundaries (one ms short/over resets, exact min/max accepted), full 7-tap success path, and mid-pattern resets (extra tap before the pause, a slow tap mid-second-burst) |
| `DragReorderTest` (9) | `DragReorder.targetIndex`/`reordered` — the hover-target math and list-move step shared by Home's reward-list and Tasks' task-list long-press-drag reorder gestures: no target while still over the dragged item's own slot, correct target over another slot, dragged item excluded even if the center falls back inside its own bounds, leading/trailing edge boundaries (exclusive), moving an item down/up shifts the in-between items correctly, and a multi-step sequence (drag down twice, up once) ends at the correct final order |
| `FieldValidationTest` (14) | `acceptWithinLimit`/`digitsOnly`/`nicknameFieldEdit`/`taskGroupFieldEdit`/`TaskEditState.withIncludedSetTo` — the character-cap, digit-only, cap-then-conditionally-reset-a-sibling-toggle, and task-link uncheck-reset transforms shared by every field that caps length, filters to digits, or resets other state on edit: under/at/over the character-cap boundary, a same-length replacement at the cap, digit-filtering of mixed/all-digit/no-digit input, the Settings nickname field disabling "random nickname" only on an accepted (non-overflow) edit, the Task group field clearing an existing-group selection only when the typed text is non-blank, and uncheck resetting both mandatory/repeatable flags regardless of prior state vs. checking leaving them untouched |
| `OnboardingStepTest` (11) | `OnboardingLogic`/`OnboardingStep` — pure state-machine logic behind the onboarding tutorial: per-step `canAdvance` gating (name non-blank, cost positive, ≥1 task linked; Intro/AwaitingSave/Outro always advance), `next`/`previous` step ordering, Outro as a terminal state for both directions, `next`/`previous` inverse for every non-terminal step, `fromIndex` round-trips every step's index |

---

## Instrumented Tests — `app/src/androidTest/` (120 tests, requires device/emulator)

**State isolation:** Every `@HiltAndroidTest` class using `createAndroidComposeRule<MainActivity>()` calls `resetAppState()` (in `TestStateReset.kt`) as the first line of its `@Before`, immediately after `hiltRule.inject()` and before any test-specific overrides (e.g. `settingsRepository.updateMaxRewardCount(...)`). This gives each test a clean database and default settings to start from, independent of what ran before it in the same instrumentation process. `RoomIntegrationBase`-based repository tests don't need this — each already gets its own fresh in-memory database per test.

**Shared flow helpers:** `UiTestActions.kt` provides `createTask(name)`, `createReward(name, cost)`, `waitForTaskDetail()`, and `waitForRewardDetail()` as `ComposeTestRule` extensions for the base-case create-and-save flows repeated across the suite. Tests that need additional fields (group, reward links, mandatory/repeatable toggles) or reach the form via a different entry point (e.g. a reward form's own "Create your own" task dialog) build those steps inline instead — the helpers cover the common path, not every variant.

**Focused coverage:** Each test verifies one thing and relies on other tests for the rest — don't re-prove logic already covered elsewhere. Assert after each state-changing action instead of chaining several before checking once at the end, so a failure points at exactly which step broke.

**Tagging convention:** Every class in `app/src/androidTest/` carries one required layer tag —
`@RepositoryTest`, `@UtilityTest`, or `@UiTest` (`com.earnit.app.tags`) — plus at least one
optional tag describing what it covers: `@Smoke` for the critical golden-path flows, or one or
more of `@Task`, `@Reward`, `@Settings`, `@Widget`, `@Nudge`, `@ImportExport`, `@CleanUp`. A class
can carry several optional tags when it spans more than one area. The layer tag drives
`instrumented-tests.yml`'s CI sharding (`-e annotation=`); any tag can be targeted directly the
same way, e.g. `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=com.earnit.app.tags.Reward`
to run only reward-related instrumented tests. `checkInstrumentedTestTags` (part of `./gradlew
check`) fails the build if any class is missing its required layer tag or has no optional tag.

| File | Layer | What it covers |
|---|---|---|
| `HappyPathTest` (1) | Repository | Create task → create reward → link as mandatory → log → assert `canClaim` → claim → assert history entry + archived log + reward archived |
| `LogAgainstArchivedRewardTest` (1) | Repository | `logCompletion` against a reward archived via `claimReward` is a no-op — no new log row is written and the existing history entry's logs are unchanged |
| `ConcurrentLogCompletionTest` (1) | Repository | Two concurrent `logCompletion` calls for the same non-repeatable `(taskId, rewardId)` write only one log row — the repository-level guard, not just the UI's loggable-state filtering, prevents the duplicate |
| `StartOverTest` (3) | Repository | `startOver=true` — reward stays active, history entry created, point balance resets to zero, second cycle immediately valid |
| `ClearCascadeTest` (5) | Repository | `clearAllLogs` removes active + archived logs and history entries; `clearAllTasks` removes cross-refs, leaves reward; `clearAllRewards` removes cross-refs + active logs, leaves task; `deleteTask` / `deleteReward` cascade |
| `ExportImportTest` (11) | Repository | Export → clear → import(replace) round-trip preserving all entity types; import(replace) preserves archived history; import(merge) preserves existing + adds new; file-based variants via temp `Uri`; malformed JSON → `ImportInvalidJsonException`; wrong-schema JSON → `ImportWrongSchemaException`; file-backed bad JSON and wrong-schema variants; wrong-schema replace attempt leaves existing DB data intact; a foreign file reusing one EarnIt key also leaves existing DB data intact |
| `NudgeDataTest` (6) | Repository | Real-Room coverage for the SQL `NudgeWorkerTest` only mocks: `getLastLogTimestamp` null with no logs / returns max among out-of-order logs; `getActiveRewardCount` zero with no rewards / counts only non-archived; `debugBackdateLastLog` caps every log newer than the cutoff (not just the single most-recent row) so the global max actually drops below it even with several near-simultaneous recent logs, leaves genuinely old logs untouched, safe no-op with no logs |
| `WidgetFlashTest` (7) | Utility | `WidgetFlash` — set/isActive round-trip; false for different reward ID; false after expiry; false when nothing set; `remainingMs` positive when active, zero after expiry, zero for wrong reward |
| `UiHappyPathTest` (1) | UI | Full Compose UI flow: create task → create reward → link from Reward Detail → log from Prizes home card → open detail → claim → verify claimed reward appears in History |
| `ProcessDeathRestoreTest` (1) | UI | Approximates a cold start with no saved-instance-state Bundle (closes the managed `ActivityScenario`, launches a brand-new one): a logged task/reward's data survives the relaunch (Room), while having navigated to the Tasks tab beforehand does not (nav back stack resets to the default start screen) |
| `SettingsUiTest` (2) | UI | Colour scheme selection persists after `activityRule.scenario.recreate()`, confirmed against the chip's rendered selection state, not just the repository; Notes required toggle disables LOG until a note is entered, enables it after |
| `EmptyStateUiTest` (1) | UI | Fresh-install empty-state copy on all three tabs: Prizes ("No rewards yet"), Tasks ("No tasks yet"), History — both Completed Tasks and Claimed Rewards sub-tabs |
| `EarnAgainButtonUiTest` (4) | UI | Claimed Rewards tab's icon-only "Earn Again" button: tapping it (found by content description, since it has no visible label) reaches `copyRewardFromEntry`, shows a confirmation snackbar, and a new active reward with the same name/cost appears on Prizes; the linked task and description carry over onto the new reward's detail screen; a second tap on the same history row shows an "already exists" snackbar instead of creating a duplicate; tapping while already at the configured Max Reward Count shows the same limit tooltip the FAB uses and does not exceed the cap — the UI wiring on top of `RepositoryBehaviourTest`'s coverage of `copyRewardFromEntry` itself |
| `TaskLibraryImportUiTest` (1) | UI | Task Library: expand "Healthy Living" template, add all 10 tasks, verify they appear in the Tasks list |
| `SaveNavigationUiTest` (6) | UI | Post-save navigation: new task → TaskDetailScreen; new reward → RewardDetailScreen; task created from new-reward form → pops back to reward form (task auto-included), both saved and linked on reward save; Add task button disabled until reward name is entered; home card's "+ ADD TASKS" shortcut and the widget's ADD TASK intent extras both open the Add Task dialog directly on Reward Detail, not Reward Edit |
| `ImportErrorUiTest` (2) | UI | Import error messages appear on Data & Backup screen: invalid JSON file shows "File is not valid JSON"; wrong-schema JSON shows "This doesn't look like an EarnIt backup" |
| `MaxLengthUiTest` (6) | UI | Reward name, task name, reward description, task group name, nickname, and widget label fields each accept input up to their character cap and silently reject one character past it |
| `WidgetNudgeUiTest` (1) | UI | Widget nudge banner on Reward Detail: hidden while a reward has no tasks, appears once the first task is linked, dismiss hides it and persists across `activityRule.scenario.recreate()` |
| `DuplicateNameUiTest` (2) | UI | Duplicate-name error shown and SAVE disabled when a task or reward name conflicts with an existing one, case-insensitive |
| `RewardLimitUiTest` (1) | UI | Tapping the reward FAB at `maxRewardCount` shows the max-limit tooltip instead of navigating to Reward Edit |
| `RewardProgressBarUiTest` (1) | UI | Reward Detail progress bar hides its point/cost number overlay once points meet the cost but a mandatory task is still unlogged (`RewardProgress.showsProgressNumbers` wiring; boundary cases unit-tested in `RewardProgressTest`) |
| `RewardAllTasksLoggedHintUiTest` (2) | UI | LOG disables and an explanatory hint appears, on both Reward Detail and the Prizes home card, once every linked task is non-repeatable and already logged (`RewardProgress.loggableTasks` wiring; boundary cases unit-tested in `RewardProgressTest`) |
| `RewardDetailTaskRowUiTest` (2) | UI | "Complete to earn points" task row: the checkmark (completion) and the ★/↻ flag icons (mandatory/repeatable) are independent — flags stay visible and unchanged across the logged/not-logged transition, and both are absent when neither flag applies (`RewardProgress.isTaskLogged` wiring; boundary cases unit-tested in `RewardProgressTest`) |
| `TaskEditScreenUiTest` (11) | UI | Delete confirmation removes the task and returns to the Tasks list; icon picker selection updates the icon button and dismisses the dialog; group picker — selecting an existing group updates the header label, typing a new group name clears that selection, clearing the new-group text reverts to the optional label, and the selected group row carries `Role.RadioButton` + selected semantics for TalkBack; auto-points sliders drive the computed total (checked against `PointFormulaTest`'s known formula output); manual points field strips non-digit input; reward-link checkbox includes/excludes the task and enables/disables the mandatory-star and repeatable-refresh toggles, which reset together when unchecked; editing an existing task's name/icon/group/points persists after Save; reopening a task already linked to a reward pre-populates the reward-link checkbox and mandatory state from its existing link; adding a task from an existing (already-saved) reward's own Detail screen shows the "used in" line instead of the checkbox list and pops back to Reward Detail with the task linked; screen Cancel pops back without saving; delete-dialog Cancel keeps the task |
| `SettingsScreenUiTest` (14) | UI | Nickname typed in Settings shows in the home greeting ("Earn It, Name!"); clearing it shows "Earn It!" with no address; enabling random nickname replaces the typed name in the greeting with an actual random nickname, not just the typed name's absence; Show Quote toggle hides/shows the daily quote section on Home; Max Reward Count defaults to 5 on a fresh install; editing Max Reward Count through the Settings slider (not the repository directly) still triggers the FAB's max-limit tooltip; mascot picker's default unlocked set is exactly Pugsly and Tabby, both selectable within the dialog; the next-locked mascot's unlock hint is shown while its name stays hidden, and further-locked mascots show neither name nor hint; selecting an unlocked mascot persists after `activityRule.scenario.recreate()`, confirmed against the rendered Settings row, not just the repository; mascot picker's backdrop/back-press dismiss (its only dismiss path — no explicit Cancel button) leaves the selection unchanged; About/Data & Backup/Clean Up rows navigate to their respective screens; About screen copy no longer claims data never leaves the phone; cloud backup toggle defaults on and persists off after `activityRule.scenario.recreate()`, confirmed against the rendered Switch state, not just the repository |
| `RewardEditScreenUiTest` (13) | UI | Delete confirmation removes the reward and returns to the Prizes list; icon picker selection updates the icon button and dismisses the dialog; cost field strips non-digit input; an included task row's mandatory-star and repeatable-refresh toggles flip their content description, and unchecking the row removes the task with mandatory/repeatable reset together; editing an existing reward's name/cost/description/icon persists after Save; reopening a reward already linked to a mandatory task pre-populates that task's mandatory icon state; two tasks added via the dialog's checkbox list in one session toggle/remove independently of each other; selecting an existing task through the dialog and setting its mandatory flag inline carries that flag through on confirm; the Browse Library button navigates to Task Library; adding two new tasks in a row via "Create your own" on an *unsaved* reward keeps both included across the round-trips; screen Cancel pops back without saving; delete-dialog Cancel keeps the reward; icon-picker Cancel keeps the previous icon |
| `OnboardingFlowUiTest` (3) | UI | First-launch onboarding tutorial end-to-end: triggers on first launch, each spotlight step's real field is reachable through the cutout, the starter chip fills the real reward-name field, the task-creation detour's two-beat coaching bubble (name, then points) and the post-link required/repeatable-icon bubble both appear at the right moment, completing the flow persists the seen flag and leaves a real reward + task on Home, and Settings → Replay Tutorial re-triggers it; a duplicate reward name (e.g. replaying and reusing a starter chip a prior run already saved) blocks Save with an explanatory message instead of a silently-disabled button |
| `CleanUpScreenUiTest` (5) | UI | Each of the four destructive-action dialogs (Clear Logs / Clear Tasks / Clear Rewards / Wipe Everything) — Cancel dismisses the dialog and clears nothing, confirmed directly against the repository rather than just the UI; Wipe Everything's Confirm path wipes all data and resets DataStore settings (theme, nickname, dev mode, mascots) to default while preserving the cloud backup opt-out |
| `TaskLibraryScreenUiTest` (1) | UI | The skipped-tasks dialog's only dismiss path (backdrop/back-press — no explicit Cancel button, only "OK" which does the same navigate-back) still applies the already-completed import correctly |
| `SharedDialogsCancelUiTest` (4) | UI | Cancel path for each dialog shared across screens: `LogTaskDialog` (no log recorded, selected task row carries `Role.RadioButton` + selected semantics before the cancel), `ClaimDialog` (reward stays active, not archived), `AddTaskToRewardDialog` (task not included), `LogForRewardDialog` (no log recorded) — none had a dedicated cancel-path test before, only confirm-path coverage |
| `LogForRewardDialogUiTest` (1) | UI | `LogForRewardDialog`'s multi-reward branch (a task linked to more than one reward): the reward picker renders both reward names, LOG stays disabled until one is selected, logging credits only the chosen reward — the other's progress is untouched — and the chosen/other rows carry `Role.RadioButton` + selected/not-selected semantics for TalkBack |
| `DragReorderUiTest` (2) | UI | Drives the real long-press-then-drag gesture via `performTouchInput` (down, an explicit stationary `advanceEventTime` past the long-press threshold, then `moveTo`) on both Home's reward cards and Tasks' task cards: dragging the first card past the other two lands it below both, asserted against actual on-screen position, not the underlying list state |

---

## Edge Cases

### Covered

**Zero-cost reward with unlogged mandatory task** (`GatekeeperTest`)
`canClaim = false` when the point threshold is met but a mandatory task has not been logged. Guards against users bypassing mandatory requirements on a free reward.

**Task attached to two rewards simultaneously** (`RewardProgressTest`, `RepositoryBehaviourTest`, `LogForRewardDialogUiTest`)
Log attribution is scoped to `(taskId, rewardId)`. Logging task T against reward R1 writes a log with `rewardId = R1`. `loggableTasks` for R2 queries `completionLogs WHERE rewardId = R2`, so T remains loggable for R2. Verified implicitly through the `loggableTasks` unit tests; the two-reward fixture is not set up explicitly at integration level — acceptable given the query isolation is straightforward Room SQL. `LogForRewardDialogUiTest` covers the UI side of the same scenario: `LogForRewardDialog`'s reward picker (shown only when a task has more than one reward) requires an explicit selection before LOG is enabled, and the resulting log is attributed to the chosen reward only.

**Deleting a reward with in-progress logs** (`DeleteCascadeTest`, `ClearCascadeTest`)
`deleteReward` removes all active logs before deleting the entity; cross-ref cleanup is handled by the FK cascade, not a manual call. `clearAllRewards` clears active logs and cross-refs across all rewards but deliberately preserves history entries from completed cycles.

**Wipe Everything resets settings except the backup choice** (`CleanUpScreenUiTest`, `WipeEverythingViewModelTest`)
Clean Up → Wipe Everything clears the database and also resets every DataStore setting (theme, nickname, dev mode, unlocked mascots, onboarding, etc.) back to default — except `cloudBackupEnabled`, which `SettingsRepository.resetForWipeEverything()` deliberately preserves since it's a privacy opt-out, not app data. `CleanUpScreenUiTest` seeds non-default values for both categories and asserts data is gone, settings are back to default, and the backup toggle survived unchanged.

**Non-repeatable task logged twice** (`RewardProgressTest`)
`loggableTasks` excludes a non-repeatable task once a log entry exists for its `(taskId, rewardId)` pair. Repeatable tasks always remain available regardless of prior logs.

**Point formula boundary: single-dimension max bonus** (`PointFormulaTest`)
The +3 bonus fires at exactly dimension value 5 and is absent at 4 — off-by-one guard on the threshold check.

**Claim-time snapshot integrity** (`ClaimRewardStartOverTest`, `LogAttributionTest`)
Task name and reward name/icon/cost are snapshotted at the moment of log and claim. History remains accurate even if the task or reward is later renamed or deleted.

**Import dedup across case and whitespace variants** (`ImportDedupTest`)
Library import skips tasks whose names match existing tasks after lowercasing and trimming. Sort order is assigned continuously, skipping gaps left by deduped entries.

**All one-time tasks logged, reward not yet claimable** (`RewardAllTasksLoggedHintUiTest`)
Once every task linked to a reward is non-repeatable and already logged, `loggableTasks` is empty and LOG has nothing left to offer. LOG is asserted disabled and a hint explaining why is shown, on both Reward Detail and the Prizes home card — otherwise the button would silently do nothing (Reward Detail) or open an empty log dialog (home card).

**Notes-mandatory enforcement** (`SettingsUiTest`)
When `notesMandatory = true`, the LOG button is asserted disabled before any note is entered and enabled after — full path from settings toggle through to dialog state.

**Empty-state screens** (`EmptyStateUiTest`)
Fresh-install copy on Prizes, Tasks, and both History sub-tabs is asserted directly against `Strings.kt` constants, so the test fails if either the copy or the empty-state condition drifts.

**Task Library import** (`TaskLibraryImportUiTest`)
Full UI path: Tasks tab → Library → expand a template → add all tasks → confirm they appear in the Tasks list. `ImportDedupTest` covers the dedup logic itself at the repository level; this test covers the UI wiring (navigation, checkbox state, button enabling) on top of it.

**Post-save navigation** (`SaveNavigationUiTest`)
Saving a new task navigates to TaskDetailScreen; saving a new reward navigates to RewardDetailScreen. Creating a task from a new-reward edit form pops back to the reward form (not forward to TaskDetailScreen), auto-includes the task in the form's task list, and persists both entities linked when the reward is subsequently saved. The home card's "+ ADD TASKS" shortcut is also asserted to land on the Add Task dialog directly rather than the Reward Edit screen, so the shortcut takes exactly one tap to reach the dialog.

**Widget nudge dismissal persists** (`WidgetNudgeUiTest`)
The one-time widget nudge banner on Reward Detail is asserted to disappear immediately on dismiss and to stay hidden after `activityRule.scenario.recreate()`, proving the DataStore flag round-trips rather than just the in-memory Compose state resetting.

**Widget action-button selection** (`WidgetActionButtonTest`, `WidgetContentTest`)
The button-state decision (`CLAIM` / `LOG` / `LOG_DISABLED` / `ADD_TASK`) is a plain function, unit-tested directly; `WidgetContentTest` renders the actual composables via `glance-testing` + Robolectric to confirm the right button (and only that button) appears with its click action wired — or, for `LOG_DISABLED`, that the button renders with no click action alongside the "all tasks done" hint icon — plus reward name/points text and the hint icon's content description. Neither test can verify the click actually reaches the intended `Intent` extras (`glance-testing`'s click-action matchers don't recognize the raw-`Intent` `actionStartActivity` overload this widget uses) — that part stays manual, per `MANUAL_TEST_PLAN.md`.

**Drag-to-reorder gesture on Home and Tasks** (`DragReorderTest`, `DragReorderUiTest`)
`DragReorderTest` unit-tests the hover-target/list-move math (`DragReorder`) shared by both screens. `DragReorderUiTest` drives the real long-press-drag via `performTouchInput` and asserts actual on-screen card order against the underlying list state, not just the model, since the two can diverge if the drag gesture's visual feedback and the list's persisted order fall out of sync.

**Character-cap, digit-filter, and task-link uncheck-reset field transforms** (`FieldValidationTest`, `MaxLengthUiTest`, cost/points digit-filter and toggle-reset cases in `RewardEditScreenUiTest`/`TaskEditScreenUiTest`)
`FieldValidationTest` unit-tests `acceptWithinLimit`, `digitsOnly`, and `TaskEditState.withIncludedSetTo` (`FieldValidation.kt`, `SharedDialogs.kt`) directly, including the character-cap and digit-filter boundary cases that previously only existed implicitly through full `MainActivity` instrumented tests. The existing UI tests continue to verify each field is actually wired to the shared functions.

**Import file validation** (`JsonImportValidationTest`, `ImportViewModelErrorTest`, `ExportImportTest`, `ImportErrorUiTest`)
`JsonExport.fromJson` requires all five top-level keys (`tasks`, `rewards`, `rewardTaskCrossRefs`, `completionLogs`, `historyEntries`) to be present in the parsed JSON, not merely any one of them — a foreign file that happens to reuse one key name is rejected as `ImportWrongSchemaException` before touching the database, which matters in Replace mode where silent failure would wipe user data. `ExportImportTest.importReplace_withWrongSchema_doesNotWipeExistingData` and `importReplace_withForeignKeyMatchingSchema_doesNotWipeExistingData` prove at integration level that existing DB rows survive both kinds of wrong-schema replace attempt (not just that the exception fires). Genuine JSON syntax errors throw `ImportInvalidJsonException`; text that's valid JSON but the wrong shape — including a recognizable key holding wrong-shaped elements — throws `ImportWrongSchemaException` rather than being mislabeled as invalid JSON. Each exception type maps to a specific user-facing string in the ViewModel and is verified against `importResult` StateFlow as well as the `onComplete` callback. UI tests verify the error messages actually appear on the Data & Backup screen.

**Cold start with no saved-instance-state Bundle** (`ProcessDeathRestoreTest`)
`SettingsUiTest` and others use `activityRule.scenario.recreate()`, which preserves the saved-instance-state Bundle — proving `rememberSaveable` fields round-trip, not that anything relies on Room/DataStore over Bundle survival. `ProcessDeathRestoreTest` instead closes the managed `ActivityScenario` and launches a brand-new one with no Bundle: a logged task/reward survives (Room), while having navigated to the Tasks tab beforehand does not (nav back stack resets to the default start screen). This is an approximation, not a literal `am force-stop` — this repo runs instrumented tests with no Test Orchestrator, so the test shares a process with the app under test, and a real force-stop would kill the test itself mid-method. It does not exercise an actual kill of Application/Hilt-singleton-scoped state, since the process itself is never terminated; this app has no such state that matters functionally today.

**Rapid double-tap logging** (`ConcurrentLogCompletionTest`)
`logCompletion` runs inside `database.withTransaction { }` and no-ops if an active log already exists for a non-repeatable `(taskId, rewardId)` pair, so a fast double-tap on LOG can't insert two log rows. Enforced independently of `RewardProgress.loggableTasks`, the UI property that decides whether the LOG button is shown.

**Logging against an archived reward** (`LogAgainstArchivedRewardTest`)
`logCompletion` fetches the reward first and returns early if it's missing or already archived, rather than inserting unconditionally. Guards a stale-UI race (e.g. a reward claimed from one surface while another still shows its LOG button) from writing an orphaned log with no `historyEntryId`; the write is silently skipped rather than surfaced as an error, since neither call site (`EarnItViewModel.logTask`, `WidgetTaskLogActivity`) currently acts on `logCompletion`'s result.

**"Earn Again" icon button on Claimed Rewards** (`EarnAgainButtonUiTest`, `RepositoryBehaviourTest`)
Before this test, `copyRewardFromEntry` had only repository-level coverage of its flag/icon-preservation logic — nothing exercised the on-screen button itself (icon-only, no visible label; `Strings.HISTORY_EARN_AGAIN` supplies its `contentDescription` instead), and manual testing surfaced four real gaps this pass closes: the tap gave no feedback, rapid re-taps could create multiple active rewards sharing the same name, the copy silently dropped the original reward's description, and it ignored the configured Max Reward Count — the cap `HomeScreen`'s FAB enforces on its own reward-creation path had no equivalent here. Both the duplicate-name and reward-count guards live inside `copyRewardFromEntry`'s own `database.withTransaction` block, checked against fresh DB reads rather than a possibly-stale in-memory snapshot — the same pattern `logCompletion`'s non-repeatable-task guard already uses and `ConcurrentLogCompletionTest` proves closes this exact class of double-tap race. The repository now returns a `CopyRewardOutcome` (`ADDED`/`NAME_CONFLICT`/`MAX_REWARDS_REACHED`, or `null` if the entry vanished) instead of a bare `Boolean`, so the UI can show the right message. `EarnAgainButtonUiTest` covers the UI wiring on top: a confirmation snackbar appears, the linked task and description show up on the new reward's detail screen, a second tap on the same row shows an "already exists" snackbar instead of a duplicate, and tapping while already at the cap shows the same `MAX_REWARD_TOOLTIP` text the FAB uses without exceeding it.

**Cancel/dismiss across every screen and dialog** (`RewardEditScreenUiTest`, `TaskEditScreenUiTest`, `SettingsScreenUiTest`, `CleanUpScreenUiTest`, `TaskLibraryScreenUiTest`, `SharedDialogsCancelUiTest`)
Every Cancel button and dialog dismiss in the app is a one-line `popBackStack()`/`onDismiss()` callback with no logic, covered here app-wide. A shared `cancelDialogAndAssertDismissed` helper (`CancelDismissAssertions.kt`) clicks a dialog's Cancel button — scoped to the dialog's own window via the `isDialog()` matcher, since several dialogs share the exact text "CANCEL" with a button on the screen behind them — and asserts the dialog is gone; each test still supplies its own setup and its own side-effect assertion (no task created, no log recorded, reward not archived, etc.). Dialogs with no explicit Cancel button (`MascotPickerDialog`, `TaskLibraryScreen`'s skipped-tasks dialog) are covered via `Espresso.pressBack()` instead, their only dismiss path.

**Radio-group TalkBack semantics** (`TaskEditScreenUiTest`, `LogForRewardDialogUiTest`, `SharedDialogsCancelUiTest`)
Every radio-option row in the app (`RadioRow` in `EarnItButtons.kt`, plus the bespoke rows in `TaskEditScreen.kt`'s custom-group entry and `SharedDialogs.kt`'s `LogTaskDialog`) uses `Modifier.selectable(role = Role.RadioButton)` with the enclosing list carrying `Modifier.selectableGroup()`, instead of a plain `Modifier.clickable` — a screen reader announces these as radio buttons with selected state, not generic tappable rows. Verified by asserting `Role.RadioButton` plus `assertIsSelected()`/`assertIsNotSelected()` on the affected rows in each of the three test files above, confirmed passing against the real merged semantics tree on a connected device (not just reasoned about) — `clickable`-based modifiers merge their descendants' semantics upward, so `onNodeWithText`/similar matchers on a row's label correctly resolve to the row's own selected/role state.

---

## Test Cadence

When each layer runs, and on what trigger. Update this table as CI/CD workflows land — most "manual" rows here become automated once Workflows 1–2 exist.

| Layer | Trigger | Command / Reference |
|---|---|---|
| Unit (205 tests) | Every build/push | `./gradlew test` |
| Integration + UI, instrumented (120 tests) | Every push/PR via CI (two parallel API 36 emulator jobs, Workflow 2 — sharded by layer); also manually before every release candidate | `./gradlew connectedDebugAndroidTest` |
| Manual-only journeys (4) | Varies per journey — see each entry | [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) |

See [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) for the journeys that are deliberately never automated (not just deferred) — each crosses a system-process boundary (system file picker, Play Core API, widget activity chain, background `WorkManager` execution) that instrumented UI tests cannot drive reliably.

---

## Deferrals

**Widget task logging**
Covered by manual testing, not automation — see [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) for rationale and steps.

**Widget refresh side effect (`refreshWidgets()`)**
`EarnItViewModel.logTask()`, `claimReward()`, and `addTaskToReward()` each call `refreshWidgets()`, which calls `EarnItGlanceWidget().updateAll(context)` by direct instantiation rather than through an injected/mockable seam — no test verifies this call actually happens. A regression here manifests as the widget silently showing stale content, not a crash or visible error, so it's easy to miss without a real device. Deferred because closing it properly means injecting a widget-refresh interface via Hilt, a real architectural change; the manual widget journey in `MANUAL_TEST_PLAN.md` is the current backstop.

**TipViewModel**
`MockTipRepository` returns hardcoded prices and always succeeds. Tests written against it would validate the mock, not the billing path. Tests deferred until `MockTipRepository` is replaced with real RevenueCat calls; the `TipRepository` interface boundary makes the swap straightforward.

**`AddTaskToRewardDialog` group view UI**
Collapse/expand state, "Other" section behaviour, select-all checkbox logic, and the name-search filter (shown once more than 7 tasks are available) are pure display/filtering state with no database writes at risk. The instrumented test setup required to drive these interactions is disproportionate to the risk. Verified manually before each release. This is narrower than it used to be: the dialog's actual task-selection mechanism — checking a task, optionally toggling its mandatory/repeatable flags inline, and confirming — is covered by `RewardEditScreenUiTest.existingTaskSelection_viaDialogCarriesMandatoryFlagThroughToIncludedList`, since that flow does carry real state into what eventually gets saved.

**Transaction rollback on partial failure**
`EarnItRepository`'s multi-step mutations (`importFromJson`, `deleteReward`, `clearAllTasks`/`clearAllRewards`/`clearAllLogs`, `importTemplate`, `copyRewardFromEntry`, `claimReward`, `saveRewardTasks`, `updateTaskRewards`) are wrapped in `database.withTransaction { }` so a crash mid-sequence can't leave the DB half-mutated. The unit tests (MockK-mocked database) verify DAO call sequencing, not real rollback — MockK can't simulate Room's actual transaction/rollback behaviour. An instrumented test against a real in-memory Room database, forcing one DAO call in a wrapped sequence to throw and asserting the rest never committed, would close this gap. Not yet written.
