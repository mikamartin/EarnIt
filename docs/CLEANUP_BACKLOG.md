# EarnIt — Cleanup Backlog

**Disposal:** once actioned, fold the result into a new numbered entry in `CLEANUP_LOG.md` and delete this file — it's a working list, not a permanent doc.

---

## Test Coverage

### Drag-to-reorder gesture on Home has no automated coverage
The long-press-and-drag reordering logic in `HomeScreen.kt`'s `homeRewardListItems` (drag start/move/end/cancel handling, mid-drag position swapping, and the final `viewModel.updateRewardsOrder` commit) has no automated test at any level — not unit, not instrumented. `SortOrderTest` covers the repository-level persistence (`updateRewardsSortOrder`) once a final order is handed to it, but nothing exercises the gesture that produces that order. Not mentioned in `TESTING.md`'s Tier 4 or "Not Covered" sections, so it's currently untested by omission rather than by a documented decision. Compose's drag-gesture testing support is limited (long-press-then-drag is nontrivial to model reliably via `performTouchInput`), so closing this gap cleanly likely means extracting the reorder-target computation (the "which item are we hovering over" math) into a plain testable function — the same pattern `WidgetActionButtonTest` used to pull decision logic out of a Glance composable — leaving only the actual `pointerInput` wiring as manual-only surface.

---