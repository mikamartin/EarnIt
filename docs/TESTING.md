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
Group-view collapse state, dialog checkbox behaviour, and widget task logging are pure UI concerns with no data at risk. Bugs are visible and fixed without data recovery. Covered by manual exploratory testing; automated tests deferred on cost/risk grounds.

---

## Test Pyramid

```
                 [ Manual — 3 journeys ]   System-boundary flows; see MANUAL_TEST_PLAN.md
            [ UI — ~10 tests ]          ComposeTestRule + Hilt, real DataStore
       [ Integration — ~30 tests ]      Real in-memory Room, no mocks
     [ Unit — 100+ tests ]              JVM, MockK DAOs, fast
```

**Run unit tests** (JVM, no device needed)
```
./gradlew test
```

**Run instrumented tests** (requires connected device or emulator)
```
./gradlew connectedDebugAndroidTest
```

**CI:** Unit tests run on every build (Workflow 1). Instrumented tests run on every push/PR via an API 34 emulator (Workflow 2) and manually before each release candidate.

---

## Unit Tests — `app/src/test/` (100+ tests)

| File | What it covers |
|---|---|
| `PointFormulaTest` (7) | `computeAutoPoints` — min, max, mixed, medium, max single-dimension bonus; `effectivePoints()` auto vs manual override |
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

---

## Instrumented Tests — `app/src/androidTest/` (~45 tests, requires device/emulator)

| File | Layer | What it covers |
|---|---|---|
| `HappyPathTest` (1) | Repository | Create task → create reward → link as mandatory → log → assert `canClaim` → claim → assert history entry + archived log + reward archived |
| `StartOverTest` (3) | Repository | `startOver=true` — reward stays active, history entry created, point balance resets to zero, second cycle immediately valid |
| `ClearCascadeTest` (5) | Repository | `clearAllLogs` removes active + archived logs and history entries; `clearAllTasks` removes cross-refs, leaves reward; `clearAllRewards` removes cross-refs + active logs, leaves task; `deleteTask` / `deleteReward` cascade |
| `ExportImportTest` (10) | Repository | Export → clear → import(replace) round-trip preserving all entity types; import(replace) preserves archived history; import(merge) preserves existing + adds new; file-based variants via temp `Uri`; malformed JSON → `ImportInvalidJsonException`; wrong-schema JSON → `ImportWrongSchemaException`; file-backed bad JSON and wrong-schema variants; wrong-schema replace attempt leaves existing DB data intact |
| `WidgetFlashTest` (7) | Utility | `WidgetFlash` — set/isActive round-trip; false for different reward ID; false after expiry; false when nothing set; `remainingMs` positive when active, zero after expiry, zero for wrong reward |
| `UiHappyPathTest` (1) | UI | Full Compose UI flow: create task → create reward → link from Reward Detail → log from Prizes home card → open detail → claim → verify claimed reward appears in History |
| `SettingsUiTest` (2) | UI | Colour scheme selection persists after `activityRule.scenario.recreate()`; Notes required toggle disables LOG until a note is entered, enables it after |
| `EmptyStateUiTest` (1) | UI | Fresh-install empty-state copy on all three tabs: Prizes ("No rewards yet"), Tasks ("No tasks yet"), History — both Completed Tasks and Claimed Rewards sub-tabs |
| `TaskLibraryImportUiTest` (1) | UI | Task Library: expand "Healthy Living" template, add all 10 tasks, verify they appear in the Tasks list |
| `SaveNavigationUiTest` (4) | UI | Post-save navigation: new task → TaskDetailScreen; new reward → RewardDetailScreen; task created from new-reward form → pops back to reward form (task auto-included), both saved and linked on reward save; Add task button disabled until reward name is entered |
| `ImportErrorUiTest` (2) | UI | Import error messages appear on Data & Backup screen: invalid JSON file shows "File is not valid JSON"; wrong-schema JSON shows "This doesn't look like an EarnIt backup" |
| `MaxLengthUiTest` (5) | UI | Reward name, task name, reward description, task group name, and nickname fields each accept input up to their character cap and silently reject one character past it |

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
Saving a new task navigates to TaskDetailScreen; saving a new reward navigates to RewardDetailScreen. Creating a task from a new-reward edit form pops back to the reward form (not forward to TaskDetailScreen), auto-includes the task in the form's task list, and persists both entities linked when the reward is subsequently saved.

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
| Unit (100+ tests) | Every build/push | `./gradlew test` |
| Integration + UI, instrumented (~45 tests) | Every push/PR via CI (API 34 emulator, Workflow 2); also manually before every release candidate | `./gradlew connectedDebugAndroidTest` |
| Manual-only journeys (3) | Varies per journey — see each entry | [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) |

See [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) for the three journeys that are deliberately never automated (not just deferred) — each crosses a system-process boundary (system file picker, Play Core API, widget activity chain) that instrumented UI tests cannot drive reliably.

---

## Deferrals

**Live instrumented-test verification on Android 16 (API 36) devices**
`ComposeTestRule` reports `IllegalStateException: No compose hierarchies found in the app` on at least one physical Android 16 device, even though `MainActivity` launches and displays successfully. Reproduces on the pre-existing `UiHappyPathTest`, not just new tests, so it's an environment/library gap rather than a test-writing bug. Ruled out as causes: espresso-core version (3.6.1 and 3.7.0 both affected), a compose-bom patch bump, and timing (an explicit 2s sleep + `waitForIdle()` before the first assertion made no difference). All instrumented tests in this suite are believed correct based on compile checks and code review; live verification is blocked until tests run against a stable, pinned API level (34 or 35) via an emulator — which CI/CD Workflow 2 will need anyway. See `DEV_PLAYBOOK.md` Known Limitations.

**Widget task logging**
Covered by manual testing, not automation — see [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) for rationale and steps.

**TipViewModel**
`MockTipRepository` returns hardcoded prices and always succeeds. Tests written against it would validate the mock, not the billing path. Tests deferred until `MockTipRepository` is replaced with real RevenueCat calls; the `TipRepository` interface boundary makes the swap straightforward.

**Group view UI**
Collapse/expand state, "Other" section behaviour, and select-all checkbox logic in `AddTaskToRewardDialog` are pure UI state with no database writes at risk. The instrumented test setup required to drive these interactions is disproportionate to the risk. Verified manually before each release.

**Transaction rollback on partial failure**
`EarnItRepository`'s multi-step mutations (`importFromJson`, `deleteReward`, `clearAllTasks`/`clearAllRewards`/`clearAllLogs`, `importTemplate`, `copyRewardFromEntry`, `claimReward`, `saveRewardTasks`, `updateTaskRewards`) are wrapped in `database.withTransaction { }` so a crash mid-sequence can't leave the DB half-mutated. The unit tests (MockK-mocked database) verify DAO call sequencing, not real rollback — MockK can't simulate Room's actual transaction/rollback behaviour. An instrumented test against a real in-memory Room database, forcing one DAO call in a wrapped sequence to throw and asserting the rest never committed, would close this gap. Not yet written.

**FK cascade delete behaviour**
`deleteTask`/`deleteReward` rely on the `RewardTaskCrossRef` FK cascade (see `Entities.kt`) to remove cross-ref rows instead of an explicit DAO call. `DeleteCascadeTest` (MockK-mocked database) only asserts the repository no longer clears cross-refs manually — it cannot verify SQLite actually cascades the delete. An instrumented test against a real in-memory Room database would close this gap. Not yet written.
