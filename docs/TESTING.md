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
                 [ Manual — 3 journeys ]   System-boundary flows; see MANUAL_TEST_PLAN.md
            [ UI — ~30 tests ]          ComposeTestRule + Hilt, real DataStore
       [ Integration — ~25 tests ]      Real in-memory Room, no mocks
     [ Unit — 150+ tests ]              JVM, MockK DAOs, fast
```

**Run unit tests** (JVM, no device needed)
```
./gradlew test
```

**Run instrumented tests** (requires connected device or emulator)
```
./gradlew connectedDebugAndroidTest
```

**CI:** Unit tests run on every build (Workflow 1). Instrumented tests run on every push/PR via two parallel API 34 emulator jobs (Workflow 2) — sharded by layer, Repository/Utility and UI — and manually before each release candidate.

---

## Unit Tests — `app/src/test/` (150+ tests)

| File | What it covers |
|---|---|
| `PointFormulaTest` (9) | `computeAutoPoints` — min, max, mixed, medium, max single-dimension bonus (time, difficulty, preparation); `effectivePoints()` auto vs manual override |
| `GatekeeperTest` (7) | `RewardProgress.canClaim` — points boundary (exact / one-below / zero-cost), multiple mandatory tasks (all done / one missing / logged twice), zero-cost with unlogged mandatory |
| `LogAttributionTest` (5) | `logCompletion` — auto-points formula applied, manual points respected, task name snapshotted at log time, `rewardId` + detail recorded, `historyEntryId` null on new log |
| `RepositoryBehaviourTest` (12) | `claimReward` (archives / no-archive / not-found), `saveRewardTasks` (correct flags, clears existing before insert), `copyRewardFromEntry` (flags and icon preserved, appended to end of list), `importTemplate` (append / clean-slate / sortOrder / group assignment), `updateTaskRewards` (removes delinked / inserts with correct flags) |
| `ImportDedupTest` (7) | `importTemplate` dedup — exact match, case-insensitive, whitespace-trimmed; non-conflicting tasks inserted; sort order continuous across skips; skipped list preserves template casing |
| `RewardProgressTest` (11) | `totalPoints`, `canClaim` (points gate / mandatory gate / combined / no mandatory), `allTasks` ordering, `loggableTasks` (unlogged / non-repeatable already logged / repeatable re-loggable / mixed set) |
| `ClaimRewardStartOverTest` (3) | `startOver=true` — history entry created, logs archived, reward name/icon/cost snapshotted |
| `DeleteCascadeTest` (2) | `deleteTask` clears cross-refs before delete; `deleteReward` clears cross-refs + active logs before delete |
| `CleanupTest` (3) | `clearAllLogs` deletes all logs (active + archived) **and** all history entries; `clearAllTasks` removes cross-refs then tasks; `clearAllRewards` removes cross-refs + active logs only (history preserved) |
| `SortOrderTest` (7) | `upsertTask` / `upsertReward` sortOrder assignment (empty and non-empty list); upsert routes existing records to update; `updateRewardsSortOrder` / `updateTasksSortOrder` assign sequential indexes |
| `JsonExportTest` (5) | `toJson` / `fromJson` round-trip for tasks, rewards, cross-refs, logs; empty JSON object throws `ImportWrongSchemaException` |
| `JsonImportValidationTest` (8) | `fromJson` error paths — malformed JSON → `InvalidJsonException`; truncated JSON → `InvalidJsonException`; wrong schema → `ImportWrongSchemaException`; null literal → `WrongSchemaException`; JSON array → `WrongSchemaException`; single EarnIt key present → succeeds; valid export round-trips; all-empty export succeeds |
| `ImportViewModelErrorTest` (7) | ViewModel error mapping — each exception type (`FileTooLarge`, `WrongFileType`, `InvalidJson`, `WrongSchema`, `Unreadable`, unknown) calls `onComplete` with the correct string; success calls `onComplete(null)` |
| `MascotUnlockTest` (8) | `Mascots.computeNewlyUnlocked` — each condition type (`ClaimsReached`, `PointsReached`, `TasksCompleted`) unlocks at threshold and not below; already-unlocked mascots not re-returned; multiple thresholds crossed simultaneously returns all |
| `InAppReviewTriggerTest` (2) | `EarnItViewModel.claimReward` — emits `triggerInAppReview` on first claim (empty history); does not emit on subsequent claims |
| `MascotNotificationTest` (3) | `claimReward` sets `hasNewMascot` when a mascot is newly unlocked; does not set it when all already unlocked; `importFromFile` silently seeds unlocked mascots without emitting a notification or setting the badge |
| `PendingRewardIdTest` (3) | `saveReward` sets `pendingRewardId` to the upserted id when creating a new reward; leaves it null when editing an existing reward; `consumePendingRewardId` clears the value |
| `WidgetActionButtonTest` (6) | `widgetActionButtonFor` — no tasks → `ADD_TASK`; unlogged task → `LOG`; repeatable task already logged → still `LOG`; non-repeatable task already logged and below cost → `NONE`; `canClaim` → `CLAIM` even with a loggable task; unlogged mandatory task blocks `CLAIM` despite points met |
| `WidgetContentTest` (12) | Renders `StandardContent`/`FlashContent`/`EmptyState`/`ClaimedState` via `glance-testing` + Robolectric (JVM, no device) — correct button shown/hidden per state with click action wired, reward name/points/custom-label text, mandatory hint shown/hidden, flash and empty/claimed state text |
| `NudgeDeciderTest` (10) | `NudgeDecider.decide` — never-logged and no-active-reward guardrails; idle under/at/over the 48h and 96h thresholds; stage 2 never re-sends (two-nudge cap); a new log after stage 1 or stage 2 resets the streak |
| `NudgeWorkerTest` (8) | `NudgeWorker.doWork()` via `androidx.work:work-testing`'s `TestListenableWorkerBuilder` + Robolectric — real notification posted with correct title/body per stage (asserted via `NotificationManager` shadow) and correct `SettingsRepository.updateNudgeState` call for each `NudgeDecider` outcome (first nudge, second nudge, no-op under threshold, no active reward, never logged, stage-2 cap, streak reset), plus the `POST_NOTIFICATIONS`-denied path (state still recorded, no notification shown) |
| `NudgeDebugToolsTest` (3) | `EarnItViewModel.debugGetLastLogIdleHours` — whole-hour idle time from a real timestamp, null when nothing's ever been logged; `debugBackdateLastLog` writes to the repository and invokes its completion callback exactly once (the ordering the "48H"/"96H" dev buttons rely on to avoid racing `NudgeWorker` against an in-flight write) |
| `PugslyGestureTest` (10) | `PugslyGesture.nextState`/`isComplete` — the tap-timing state machine behind the secret mascot gesture: group-gap boundary (exact pass, one ms over resets), pause-window boundaries (one ms short/over resets, exact min/max accepted), full 7-tap success path, and mid-pattern resets (extra tap before the pause, a slow tap mid-second-burst) |

---

## Instrumented Tests — `app/src/androidTest/` (~60 tests, requires device/emulator)

**State isolation:** Every `@HiltAndroidTest` class using `createAndroidComposeRule<MainActivity>()` calls `resetAppState()` (in `TestStateReset.kt`) as the first line of its `@Before`, immediately after `hiltRule.inject()` and before any test-specific overrides (e.g. `settingsRepository.updateMaxRewardCount(...)`). This gives each test a clean database and default settings to start from, independent of what ran before it in the same instrumentation process. `RoomIntegrationBase`-based repository tests don't need this — each already gets its own fresh in-memory database per test.

| File | Layer | What it covers |
|---|---|---|
| `HappyPathTest` (1) | Repository | Create task → create reward → link as mandatory → log → assert `canClaim` → claim → assert history entry + archived log + reward archived |
| `StartOverTest` (3) | Repository | `startOver=true` — reward stays active, history entry created, point balance resets to zero, second cycle immediately valid |
| `ClearCascadeTest` (5) | Repository | `clearAllLogs` removes active + archived logs and history entries; `clearAllTasks` removes cross-refs, leaves reward; `clearAllRewards` removes cross-refs + active logs, leaves task; `deleteTask` / `deleteReward` cascade |
| `ExportImportTest` (10) | Repository | Export → clear → import(replace) round-trip preserving all entity types; import(replace) preserves archived history; import(merge) preserves existing + adds new; file-based variants via temp `Uri`; malformed JSON → `ImportInvalidJsonException`; wrong-schema JSON → `ImportWrongSchemaException`; file-backed bad JSON and wrong-schema variants; wrong-schema replace attempt leaves existing DB data intact |
| `NudgeDataTest` (6) | Repository | Real-Room coverage for the SQL `NudgeWorkerTest` only mocks: `getLastLogTimestamp` null with no logs / returns max among out-of-order logs; `getActiveRewardCount` zero with no rewards / counts only non-archived; `debugBackdateLastLog` caps every log newer than the cutoff (not just the single most-recent row) so the global max actually drops below it even with several near-simultaneous recent logs, leaves genuinely old logs untouched, safe no-op with no logs |
| `WidgetFlashTest` (7) | Utility | `WidgetFlash` — set/isActive round-trip; false for different reward ID; false after expiry; false when nothing set; `remainingMs` positive when active, zero after expiry, zero for wrong reward |
| `UiHappyPathTest` (1) | UI | Full Compose UI flow: create task → create reward → link from Reward Detail → log from Prizes home card → open detail → claim → verify claimed reward appears in History |
| `SettingsUiTest` (2) | UI | Colour scheme selection persists after `activityRule.scenario.recreate()`; Notes required toggle disables LOG until a note is entered, enables it after |
| `EmptyStateUiTest` (1) | UI | Fresh-install empty-state copy on all three tabs: Prizes ("No rewards yet"), Tasks ("No tasks yet"), History — both Completed Tasks and Claimed Rewards sub-tabs |
| `TaskLibraryImportUiTest` (1) | UI | Task Library: expand "Healthy Living" template, add all 10 tasks, verify they appear in the Tasks list |
| `SaveNavigationUiTest` (5) | UI | Post-save navigation: new task → TaskDetailScreen; new reward → RewardDetailScreen; task created from new-reward form → pops back to reward form (task auto-included), both saved and linked on reward save; Add task button disabled until reward name is entered; home card's "+ ADD TASKS" shortcut opens the Add Task dialog directly on Reward Detail, not Reward Edit |
| `ImportErrorUiTest` (2) | UI | Import error messages appear on Data & Backup screen: invalid JSON file shows "File is not valid JSON"; wrong-schema JSON shows "This doesn't look like an EarnIt backup" |
| `MaxLengthUiTest` (5) | UI | Reward name, task name, reward description, task group name, and nickname fields each accept input up to their character cap and silently reject one character past it |
| `WidgetNudgeUiTest` (1) | UI | Widget nudge banner on Reward Detail: hidden while a reward has no tasks, appears once the first task is linked, dismiss hides it and persists across `activityRule.scenario.recreate()` |
| `SettingsTipUiTest` (1) | UI | Settings discoverability tip: shown on first visit, dismiss hides it and persists across `activityRule.scenario.recreate()` and subsequent visits |
| `DuplicateNameUiTest` (2) | UI | Duplicate-name error shown and SAVE disabled when a task or reward name conflicts with an existing one, case-insensitive |
| `RewardLimitUiTest` (1) | UI | Tapping the reward FAB at `maxRewardCount` shows the max-limit tooltip instead of navigating to Reward Edit |
| `TaskEditScreenUiTest` (9) | UI | Delete confirmation removes the task and returns to the Tasks list; icon picker selection updates the icon button and dismisses the dialog; group picker — selecting an existing group updates the header label, typing a new group name clears that selection, clearing the new-group text reverts to the optional label; auto-points sliders drive the computed total (checked against `PointFormulaTest`'s known formula output); manual points field strips non-digit input; reward-link checkbox includes/excludes the task and enables/disables the mandatory-star and repeatable-refresh toggles, which reset together when unchecked; editing an existing task's name/icon/group/points persists after Save; reopening a task already linked to a reward pre-populates the reward-link checkbox and mandatory state from its existing link; adding a task from an existing (already-saved) reward's own Detail screen shows the "used in" line instead of the checkbox list and pops back to Reward Detail with the task linked |

---

## Edge Cases

### Covered

**Zero-cost reward with unlogged mandatory task** (`GatekeeperTest`)
`canClaim = false` when the point threshold is met but a mandatory task has not been logged. Guards against users bypassing mandatory requirements on a free reward.

**Task attached to two rewards simultaneously** (`RewardProgressTest`, `RepositoryBehaviourTest`)
Log attribution is scoped to `(taskId, rewardId)`. Logging task T against reward R1 writes a log with `rewardId = R1`. `loggableTasks` for R2 queries `completionLogs WHERE rewardId = R2`, so T remains loggable for R2. Verified implicitly through the `loggableTasks` unit tests; the two-reward fixture is not set up explicitly at integration level — acceptable given the query isolation is straightforward Room SQL.

**Deleting a reward with in-progress logs** (`DeleteCascadeTest`, `ClearCascadeTest`)
`deleteReward` removes cross-refs and all active logs before deleting the entity. `clearAllRewards` does the same across all rewards but deliberately preserves history entries from completed cycles.

**Non-repeatable task logged twice** (`RewardProgressTest`)
`loggableTasks` excludes a non-repeatable task once a log entry exists for its `(taskId, rewardId)` pair. Repeatable tasks always remain available regardless of prior logs.

**Point formula boundary: single-dimension max bonus** (`PointFormulaTest`)
The +3 bonus fires at exactly dimension value 5 and is absent at 4 — off-by-one guard on the threshold check.

**Claim-time snapshot integrity** (`ClaimRewardStartOverTest`, `LogAttributionTest`)
Task name and reward name/icon/cost are snapshotted at the moment of log and claim. History remains accurate even if the task or reward is later renamed or deleted.

**Import dedup across case and whitespace variants** (`ImportDedupTest`)
Library import skips tasks whose names match existing tasks after lowercasing and trimming. Sort order is assigned continuously, skipping gaps left by deduped entries.

**Notes-mandatory enforcement** (`SettingsUiTest`)
When `notesMandatory = true`, the LOG button is asserted disabled before any note is entered and enabled after — full path from settings toggle through to dialog state.

**Empty-state screens** (`EmptyStateUiTest`)
Fresh-install copy on Prizes, Tasks, and both History sub-tabs is asserted directly against `Strings.kt` constants, so the test fails if either the copy or the empty-state condition drifts.

**Task Library import** (`TaskLibraryImportUiTest`)
Full UI path: Tasks tab → Library → expand a template → add all tasks → confirm they appear in the Tasks list. `ImportDedupTest` covers the dedup logic itself at the repository level; this test covers the UI wiring (navigation, checkbox state, button enabling) on top of it.

**Post-save navigation** (`SaveNavigationUiTest`)
Saving a new task navigates to TaskDetailScreen; saving a new reward navigates to RewardDetailScreen. Creating a task from a new-reward edit form pops back to the reward form (not forward to TaskDetailScreen), auto-includes the task in the form's task list, and persists both entities linked when the reward is subsequently saved. The home card's "+ ADD TASKS" shortcut is also asserted to land on the Add Task dialog directly rather than the Reward Edit screen — the same regression class as the other cases in this file (an extra tap silently inserted into a flow that should be one tap).

**Onboarding nudge dismissal persists** (`WidgetNudgeUiTest`, `SettingsTipUiTest`)
Both one-time nudges (widget nudge on Reward Detail, discoverability tip on Settings) are asserted to disappear immediately on dismiss and to stay hidden after `activityRule.scenario.recreate()`, proving the DataStore flag round-trips rather than just the in-memory Compose state resetting.

**Widget action-button selection** (`WidgetActionButtonTest`, `WidgetContentTest`)
Added after a manual test caught the widget showing no action button at all following a task add via the widget's own new entry point (see `addTaskToReward` not refreshing the widget, fixed in the same change). The button-state decision (`CLAIM` / `LOG` / `ADD_TASK` / `NONE`) was extracted out of the Glance composable into a plain function so it's directly unit-testable; `WidgetContentTest` then renders the actual composables via `glance-testing` + Robolectric to confirm the right button (and only that button) appears with its click action wired, plus reward name/points/hint text. Neither test can verify the click actually reaches the intended `Intent` extras (`glance-testing`'s click-action matchers don't recognize the raw-`Intent` `actionStartActivity` overload this widget uses) — that part stays manual, per `MANUAL_TEST_PLAN.md`.

**Import file validation** (`JsonImportValidationTest`, `ImportViewModelErrorTest`, `ExportImportTest`, `ImportErrorUiTest`)
Wrong-schema JSON (e.g. a random JSON file) throws `ImportWrongSchemaException` before touching the database — critical in Replace mode where silent failure would wipe user data. `ExportImportTest.importReplace_withWrongSchema_doesNotWipeExistingData` proves at integration level that existing DB rows survive a wrong-schema replace attempt (not just that the exception fires). Malformed JSON (invalid syntax even with an EarnIt key present) throws `ImportInvalidJsonException`. Each exception type maps to a specific user-facing string in the ViewModel and is verified against `importResult` StateFlow as well as the `onComplete` callback. UI tests verify the error messages actually appear on the Data & Backup screen.

### Not Covered by Automated Tests

**Logging against an archived reward**
Once a reward is archived, its detail screen is unreachable through the normal UI flow, so the LOG button cannot be reached. `logCompletion` itself has no active-status guard — a direct call would write an orphaned log. Acceptable given UI-level prevention; worth a repository guard and test if archived rewards are ever made re-accessible.

**Rapid double-tap logging**
Tapping LOG twice in quick succession could theoretically insert duplicate log entries before ViewModel state updates. In practice, `viewModelScope.launch` serialises DAO writes through a single coroutine dispatcher and the LOG button's enabled state re-evaluates after each Room Flow emission. Not tested; the risk is low in a local single-user app and has not surfaced in manual testing.

**True process death and restore**
`SettingsUiTest` uses `activityRule.scenario.recreate()` to cover config changes (DataStore and ViewModel re-read). True process death — OS kills the process, user returns from Recents — is not tested; this would require UiAutomator shell commands to force-stop mid-session. `rememberSaveable` guards search query, note text fields, all `RewardEditScreen` form fields, and the `awaitingNewTask` flag (so the create-task-from-reward flow survives rotation mid-flow). Dialog checkbox state (`taskState` in `RewardEditScreen`) uses `remember` and resets on config change; acknowledged in `DEV_PLAYBOOK.md`.

---

## Test Cadence

When each layer runs, and on what trigger. Update this table as CI/CD workflows land — most "manual" rows here become automated once Workflows 1–2 exist.

| Layer | Trigger | Command / Reference |
|---|---|---|
| Unit (150+ tests) | Every build/push | `./gradlew test` |
| Integration + UI, instrumented (~60 tests) | Every push/PR via CI (two parallel API 34 emulator jobs, Workflow 2 — sharded by layer); also manually before every release candidate | `./gradlew connectedDebugAndroidTest` |
| Manual-only journeys (4) | Varies per journey — see each entry | [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) |

See [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) for the journeys that are deliberately never automated (not just deferred) — each crosses a system-process boundary (system file picker, Play Core API, widget activity chain, background `WorkManager` execution) that instrumented UI tests cannot drive reliably.

---

## Deferrals

**Live instrumented-test verification on Android 16 (API 36) devices**
`ComposeTestRule` reports `IllegalStateException: No compose hierarchies found in the app` on at least one physical Android 16 device, even though `MainActivity` launches and displays successfully. Reproduces on the pre-existing `UiHappyPathTest`, not just new tests, so it's an environment/library gap rather than a test-writing bug. Ruled out as causes: espresso-core version (3.6.1 and 3.7.0 both affected), a compose-bom patch bump, and timing (an explicit 2s sleep + `waitForIdle()` before the first assertion made no difference). All instrumented tests in this suite are believed correct based on compile checks and code review; live verification is blocked until tests run against a stable, pinned API level (34 or 35) via an emulator — which CI/CD Workflow 2 will need anyway. See `DEV_PLAYBOOK.md` Known Limitations.

**Widget task logging**
Covered by manual testing, not automation — see [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) for rationale and steps.

**Widget refresh side effect (`refreshWidgets()`)**
`EarnItViewModel.logTask()`, `claimReward()`, and `addTaskToReward()` each call `refreshWidgets()`, which calls `EarnItGlanceWidget().updateAll(context)` by direct instantiation rather than through an injected/mockable seam — no test verifies this call actually happens. A regression here (as happened with `addTaskToReward` — fixed in `fix/add-task-shortcut`) manifests as the widget silently showing stale content, not a crash or visible error, so it's easy to miss without a real device. Deferred because closing it properly means injecting a widget-refresh interface via Hilt, a real architectural change; the manual widget journey in `MANUAL_TEST_PLAN.md` is the current backstop.

**TipViewModel**
`MockTipRepository` returns hardcoded prices and always succeeds. Tests written against it would validate the mock, not the billing path. Tests deferred until `MockTipRepository` is replaced with real RevenueCat calls; the `TipRepository` interface boundary makes the swap straightforward.

**Group view UI**
Collapse/expand state, "Other" section behaviour, and select-all checkbox logic in `AddTaskToRewardDialog` are pure UI state with no database writes at risk. The instrumented test setup required to drive these interactions is disproportionate to the risk. Verified manually before each release.

**Transaction rollback on partial failure**
`EarnItRepository`'s multi-step mutations (`importFromJson`, `deleteReward`, `clearAllTasks`/`clearAllRewards`/`clearAllLogs`, `importTemplate`, `copyRewardFromEntry`, `claimReward`, `saveRewardTasks`, `updateTaskRewards`) are wrapped in `database.withTransaction { }` so a crash mid-sequence can't leave the DB half-mutated. The unit tests (MockK-mocked database) verify DAO call sequencing, not real rollback — MockK can't simulate Room's actual transaction/rollback behaviour. An instrumented test against a real in-memory Room database, forcing one DAO call in a wrapped sequence to throw and asserting the rest never committed, would close this gap. Not yet written.

**FK cascade delete behaviour**
`deleteTask`/`deleteReward` rely on the `RewardTaskCrossRef` FK cascade (see `Entities.kt`) to remove cross-ref rows instead of an explicit DAO call. `DeleteCascadeTest` (MockK-mocked database) only asserts the repository no longer clears cross-refs manually — it cannot verify SQLite actually cascades the delete. An instrumented test against a real in-memory Room database would close this gap. Not yet written.
