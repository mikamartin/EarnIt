# EarnIt — Cleanup Backlog

**Disposal:** once actioned, fold the result into a new numbered entry in `CLEANUP_LOG.md` and delete this file — it's a working list, not a permanent doc.

---

## Correctness

### Adding two new tasks in a row to an unsaved reward silently drops the first one
On a brand-new, not-yet-saved reward, tapping "Add task" → "Create your own" twice in a row (creating and saving two separate new tasks before saving the reward itself) results in only the *second* task staying included — the first silently disappears from the list.

**Root cause:** `RewardEditScreen.kt`'s `taskState`/`taskStateReady` are plain `remember`, not `rememberSaveable`. Each "Create your own" round-trip navigates to `TaskEditScreen` and back, and Navigation Compose disposes `RewardEditScreen`'s composition while it's off the back stack — so both reset. The startup `LaunchedEffect` then re-derives every task's inclusion from the (still-unsaved, `cur == null`) reward, defaulting everything — including the task the user already added — back to not-included. Only the *current* `pendingTaskId` (which does survive, via `rememberSaveable`) gets explicitly re-included afterward, so each new "create new task" round-trip clobbers whichever task was added before it.

**Current status:** pinned, not fixed. `RewardEditScreenUiTest.sequentialCreateNewTasks_onUnsavedReward_onlyLastOneStaysIncluded` documents today's actual (buggy) behavior as a red-test-in-waiting rather than leaving it silently uncovered. Fixing it properly likely means making `taskState` survive the round-trip (e.g. a custom `Saver`, or restructuring so the reward is saved as a draft before the first task is added) — a real behavior change, out of scope for a structural-split branch.

---

## Visual Polish

### Reward Detail progress bar shows the current-points number in dark, muted text at low progress
`RewardDetailScreen.kt`'s progress bar (~line 431) has two rendering modes for the current-points number depending on fill level. At progress ≤ 12%, the number is drawn to the left of the fill, over the light cream unfilled track, colored `MaterialTheme.colorScheme.onSurfaceVariant` — a dark, muted brownish-gray in the app's warm color schemes. Once progress exceeds 12%, the number instead sits inside the colored fill in plain white. A reward with little progress logged so far (e.g. "Nice dinner" in test data) reads as dull/off compared to one further along (e.g. "New trainers"), which shows the crisp white variant.

**Desired fix:** always render the number inside the colored fill in white, regardless of progress level, instead of switching to the dark-text/outside-track mode below the 12% threshold — likely means anchoring the number just past the start of the fill (with a minimum offset) rather than branching on `progress <= 0.12f`.

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

### Drag-to-reorder gesture on Home has no automated coverage
The long-press-and-drag reordering logic in `HomeScreen.kt`'s `homeRewardListItems` (drag start/move/end/cancel handling, mid-drag position swapping, and the final `viewModel.updateRewardsOrder` commit) has no automated test at any level — not unit, not instrumented. `SortOrderTest` covers the repository-level persistence (`updateRewardsSortOrder`) once a final order is handed to it, but nothing exercises the gesture that produces that order. Not mentioned in `TESTING.md`'s Tier 4 or "Not Covered" sections, so it's currently untested by omission rather than by a documented decision. Compose's drag-gesture testing support is limited (long-press-then-drag is nontrivial to model reliably via `performTouchInput`), so closing this gap cleanly likely means extracting the reorder-target computation (the "which item are we hovering over" math) into a plain testable function — the same pattern `WidgetActionButtonTest` used to pull decision logic out of a Glance composable — leaving only the actual `pointerInput` wiring as manual-only surface.

---