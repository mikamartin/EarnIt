# EarnIt — Cleanup Backlog

**Disposal:** once actioned, fold the result into a new numbered entry in `CLEANUP_LOG.md` and delete this file — it's a working list, not a permanent doc.

---

## Test Coverage

### Logging against an archived reward has no repository guard, and no test
`EarnItRepository.logCompletion(task, rewardId, detail)` (`EarnItRepository.kt:130`) never checks whether the reward is archived — it just inserts. Trivially testable today with no new dependencies: a `RoomIntegrationBase`-style test (same pattern as `HappyPathTest`) archives a reward, calls `logCompletion` directly against it (bypassing the UI, which is the whole point), and asserts what happens. Either documents the current orphaned-log behavior as accepted, or becomes the red test for a guard if one is wanted.

### Rapid double-tap logging — likely a real gap, not just a coverage gap
`EarnItViewModel.logTask()` (`EarnItViewModel.kt:166`) is `viewModelScope.launch { repository.logCompletion(...) }` with no loggability re-check before the write. `TESTING.md`'s current framing ("DAO writes are serialized, button re-evaluates after each Flow emission") explains why concurrent writes don't corrupt each other, but doesn't actually prevent two `logTask()` calls for the same non-repeatable task both succeeding before the Flow emission disables the button. Not testable via real screen-tap timing (Compose's `performClick()` waits for idle by design, which defeats the exact race being tested) — instead, a repository-level test launching two concurrent `logCompletion` calls for the same non-repeatable `(taskId, rewardId)` and asserting the resulting log count tests the real invariant directly. Expected result going in: 2 logs get written, meaning the risk is real, not just theoretical — worth confirming either way before deciding if a guard is warranted.

### Drag-to-reorder gesture on Home has no automated coverage
The long-press-and-drag reordering logic in `HomeScreen.kt`'s `homeRewardListItems` (drag start/move/end/cancel handling, mid-drag position swapping, and the final `viewModel.updateRewardsOrder` commit) has no automated test at any level — not unit, not instrumented. `SortOrderTest` covers the repository-level persistence (`updateRewardsSortOrder`) once a final order is handed to it, but nothing exercises the gesture that produces that order. Not mentioned in `TESTING.md`'s Tier 4 or "Not Covered" sections, so it's currently untested by omission rather than by a documented decision. Compose's drag-gesture testing support is limited (long-press-then-drag is nontrivial to model reliably via `performTouchInput`), so closing this gap cleanly likely means extracting the reorder-target computation (the "which item are we hovering over" math) into a plain testable function — the same pattern `WidgetActionButtonTest` used to pull decision logic out of a Glance composable — leaving only the actual `pointerInput` wiring as manual-only surface.

---