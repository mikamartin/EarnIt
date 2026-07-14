# EarnIt — Cleanup Backlog

**Disposal:** once actioned, fold the result into a new numbered entry in `CLEANUP_LOG.md` and delete this file — it's a working list, not a permanent doc.

---

## Complexity & Pattern Health

### single oversized composables with no internal extraction
Well past the ~150-line guideline, comparable in scope to the `EarnItApp.kt` split done in Pass 10. Extraction pattern: pull screen-only pieces into `private` composables in the same file (matches the existing convention in `DataScreen.kt`/`TaskEditScreen.kt`), no new files/folders, no behavior changes — pure structural split, verified via ktlint + `assembleDebug` + `./gradlew test` + a manual exploratory pass per screen.

| File | Size | Status |
|---|---|---|
| `HomeScreen.kt` | ~320 lines in `HomeScreen(...)` | Planned — `refactor/split-home-screen` |

---

## Correctness

### Adding two new tasks in a row to an unsaved reward silently drops the first one
On a brand-new, not-yet-saved reward, tapping "Add task" → "Create your own" twice in a row (creating and saving two separate new tasks before saving the reward itself) results in only the *second* task staying included — the first silently disappears from the list.

**Root cause:** `RewardEditScreen.kt`'s `taskState`/`taskStateReady` are plain `remember`, not `rememberSaveable`. Each "Create your own" round-trip navigates to `TaskEditScreen` and back, and Navigation Compose disposes `RewardEditScreen`'s composition while it's off the back stack — so both reset. The startup `LaunchedEffect` then re-derives every task's inclusion from the (still-unsaved, `cur == null`) reward, defaulting everything — including the task the user already added — back to not-included. Only the *current* `pendingTaskId` (which does survive, via `rememberSaveable`) gets explicitly re-included afterward, so each new "create new task" round-trip clobbers whichever task was added before it.

**Current status:** pinned, not fixed. `RewardEditScreenUiTest.sequentialCreateNewTasks_onUnsavedReward_onlyLastOneStaysIncluded` documents today's actual (buggy) behavior as a red-test-in-waiting rather than leaving it silently uncovered. Fixing it properly likely means making `taskState` survive the round-trip (e.g. a custom `Saver`, or restructuring so the reward is saved as a draft before the first task is added) — a real behavior change, out of scope for a structural-split branch.

---

## Test Coverage

### Logging against an archived reward has no repository guard, and no test
`EarnItRepository.logCompletion(task, rewardId, detail)` (`EarnItRepository.kt:130`) never checks whether the reward is archived — it just inserts. Trivially testable today with no new dependencies: a `RoomIntegrationBase`-style test (same pattern as `HappyPathTest`) archives a reward, calls `logCompletion` directly against it (bypassing the UI, which is the whole point), and asserts what happens. Either documents the current orphaned-log behavior as accepted, or becomes the red test for a guard if one is wanted.

### Rapid double-tap logging — likely a real gap, not just a coverage gap
`EarnItViewModel.logTask()` (`EarnItViewModel.kt:166`) is `viewModelScope.launch { repository.logCompletion(...) }` with no loggability re-check before the write. `TESTING.md`'s current framing ("DAO writes are serialized, button re-evaluates after each Flow emission") explains why concurrent writes don't corrupt each other, but doesn't actually prevent two `logTask()` calls for the same non-repeatable task both succeeding before the Flow emission disables the button. Not testable via real screen-tap timing (Compose's `performClick()` waits for idle by design, which defeats the exact race being tested) — instead, a repository-level test launching two concurrent `logCompletion` calls for the same non-repeatable `(taskId, rewardId)` and asserting the resulting log count tests the real invariant directly. Expected result going in: 2 logs get written, meaning the risk is real, not just theoretical — worth confirming either way before deciding if a guard is warranted.

### True process death and restore — automatable with one new test dependency
No `androidx.test.uiautomator` dependency currently exists in `app/build.gradle.kts`. Adding it (same category as `androidx.work:work-testing` added in Pass 33) enables `UiDevice.executeShellCommand("am force-stop com.earnit.app")` — the identical command `MANUAL_TEST_PLAN.md` step 6 already has a human run manually, now scriptable. Caveat to keep in the test/doc once written: `am force-stop` is a harder kill than the OS reclaiming memory while the app sits in Recents — it also discards the saved-instance-state Bundle, so it robustly proves "cold start, state must come from Room/DataStore" but doesn't necessarily reproduce the softer "swiped away, Bundle preserved" scenario `TESTING.md`'s existing note describes ("user returns from Recents"). Worth being explicit about which of the two the new test actually proves.

### Cancel/dismiss buttons are untested across the entire app, not just one screen
 every screen's Cancel button and every dialog's dismiss button (dialog Cancel/backdrop-dismiss included) across the whole app is untested. Each one is a one-line `popBackStack()`/`onDismiss()` callback with no logic, which is why none have been covered so far — but "untested everywhere" is itself worth deciding on deliberately rather than by accretion. Options if picked up: a shared parametrized test helper, or accept the current risk explicitly in `TESTING.md` with a stated rationale (matching the `AddTaskToRewardDialog` group-view precedent) instead of leaving it merely unmentioned.

---