# EarnIt — QA Audit Backlog

Tracks findings from the most recent QA audit (see [QA_AUDIT_RULES.md](QA_AUDIT_RULES.md) for
the procedure). When a listed follow-up branch lands, don't delete its entry — condense it:
shrink its Issues Found entry to one sentence stating what it was and that it's fixed (keep the
number, so it stays findable), and replace its Work Item's Steps with a short, dry summary of
what was actually done, marked `(done)`.

## Current State

164 unit tests (`app/src/test/`), 107 instrumented tests (`app/src/androidTest/`), grown
incrementally across ~50 cleanup passes. No shared flow-helper library exists for Compose UI
tests, unlike the repository tier (`RoomIntegrationBase`) and ViewModel tier
(`ViewModelTestBase`), which do have shared base classes.

## What's Working

- 7 of 8 mutation checks caught their injected bug cleanly (`GatekeeperTest`,
  `RewardProgressTest`, `DragReorderTest`, `JsonImportValidationTest`, `NudgeDeciderTest`,
  `WidgetActionButtonTest`, `ImportDedupTest`) — regression-catching rigor holds for the core
  mechanics these files cover.
- Core mechanics — the auto-point formula, `canClaim` gating, `isRepeatable` semantics, and
  import merge/replace — all match `EARNIT_SPEC.md`'s stated behavior exactly; no test-vs-spec
  drift found.
- Structural review of the largest/newest UI test files found the suite generally follows its
  own "assert after each action" rule; only one under-assertion turned up (Issue 8).
- Per-file test counts in TESTING.md are accurate against actual `@Test` counts, spot-checked
  across all 24 unit and 29 instrumented test files.
- Existing shared test infrastructure (`RoomIntegrationBase`, `ViewModelTestBase`,
  `CancelDismissAssertions`, `TestStateReset`) is well-factored and used consistently — no ad
  hoc duplication in the repository or ViewModel tiers.
- The unit-test CI cadence claim ("every build/push") checks out — `ci.yml` runs the full suite
  unconditionally, unlike the instrumented workflow (Issue 1).

## Issues Found

1. **CI matrix omits ~40% of instrumented tests.** `instrumented-tests.yml`'s two shards
   hardcode 18 of 29 test classes (60 of 107 tests). Missing: `RewardProgressBarUiTest`,
   `RewardAllTasksLoggedHintUiTest`, `CleanUpScreenUiTest`, `ProcessDeathRestoreTest`,
   `RewardEditScreenUiTest` (13 tests), `SettingsScreenUiTest` (12 tests),
   `SharedDialogsCancelUiTest`, `TaskLibraryScreenUiTest`, `LogAgainstArchivedRewardTest`,
   `ConcurrentLogCompletionTest`, `DragReorderUiTest`. These never run on push/PR, contradicting
   TESTING.md's "every push/PR" claim. `CLEANUP_RULES.md`'s Tests checklist has no item to keep
   the CI matrix in sync with new instrumented test files.

2. **The auto-point formula is implemented twice; only one copy is directly tested.**
   `TaskEntity.computeAutoPoints()` (`Entities.kt`) and `EarnItRepository.computeAutoPoints()`
   are separate, currently-identical implementations of the same formula. `PointFormulaTest`'s
   7 formula-boundary assertions all call `repository.computeAutoPoints(...)` — the copy used
   only for the live slider preview in `TaskEditScreen` (via `EarnItViewModel.computeAutoPoints`).
   The copy actually used to award points — `TaskEntity.effectivePoints()` →
   `TaskEntity.computeAutoPoints()`, called from `EarnItRepository.logCompletion` and every
   points-display call site (`RewardDetailScreen`, `TaskDetailScreen`, `SharedDialogs`,
   `WidgetTaskLogActivity`) — has no test exercising its own bonus-threshold boundary.
   **Verified empirically**: mutating the `== 5` bonus condition in `TaskEntity.computeAutoPoints()`
   to `== 4` left the full unit test suite green (9/9 `PointFormulaTest` cases passed unchanged).
   A regression in the formula actually used to award points could ship undetected.

3. **TESTING.md's aggregate test counts disagreed with each other** (pyramid ~70/~30, section
   header ~105, cadence table ~75, actual 107). Fixed on `chore/qa-audit`.

4. **Several TESTING.md Edge Case entries narrated bug-discovery history instead of stating
   current behavior.** Fixed on `chore/qa-audit`.

5. **CLEANUP_LOG.md entries were out of order** (49 → 47 → 48 instead of 47 → 48 → 49). Fixed
   on `chore/qa-audit`.

6. **DEV_PLAYBOOK.md referenced retired cleanup passes** ("Pass 36", "Pass 5") not in the
   retained (3-most-recent) CLEANUP_LOG.md. Fixed on `chore/qa-audit`.

7. **No shared UI test flow-helper library.** "Create a task," "create a reward," and "wait for
   task/reward detail screen" are duplicated inline across roughly 14-18 of ~28 Compose UI test
   files. `DragReorderUiTest` reimplements private copies rather than sharing.

8. **`RewardEditScreenUiTest.editExistingReward_updatesFieldsAndPersists` under-asserts its own
   name.** It edits four fields (name, cost, description, icon) but only asserts two survived
   (name, icon) — a regression in cost or description persistence would pass this test silently.
   Its sibling `TaskEditScreenUiTest.editExistingTask_updatesFieldsAndPersists` asserts all four
   of its edited fields (name, icon, group, points) correctly; the reward version should match.

9. **Field-validation logic is inline in composables, not pure functions, so it can only be
   tested through full instrumented UI tests.** Verified by reading every call site:
   - **Character-cap truncation** — `{ if (it.length <= MAX) onChange(it) }` — is inlined at 8
     `onValueChange` sites (`RewardEditScreen.kt` name/description, `TaskEditScreen.kt`
     name/group, `SettingsScreen.kt` nickname, `SharedDialogs.kt` and `TasksScreen.kt` note,
     `WidgetConfigActivity.kt` label). `MaxLengthUiTest` exercises 5 of these fields, each via a
     full `MainActivity` + Hilt + Compose launch, to verify a one-line boundary check.
   - **Digit-only filtering** — `{ onChange(it.filter { c -> c.isDigit() }) }` — is inlined at 2
     sites (`RewardEditScreen.kt:472` cost field, `TaskEditScreen.kt:642` manual points field),
     each with its own full-UI test (`costField_digitFilterStripsNonDigits`,
     `manualPoints_digitFilterStripsNonDigits`).
   - **Task-link uncheck resets `included`/`isMandatory`/`isRepeatable` together** — implemented
     as the same-shaped inline `.copy(...)` transformation independently in both
     `RewardEditScreen.kt:530` and `TaskEditScreen.kt:709-714` — duplicated, not shared, the same
     class of drift the project's own Pass 49 (`CLEANUP_LOG.md`) already fixed once for
     drag-gesture math via `DragReorder`. Covered today only through
     `RewardEditScreenUiTest.taskRow_mandatoryRepeatableTogglesAndUncheckRemoves` and
     `TaskEditScreenUiTest.rewardLinks_checkboxAndMandatoryRepeatableToggles`, two independent
     multi-step UI tests instead of one shared unit test.

   None of this logic needs Compose or Hilt to verify — it's straightforward input
   transformation. Testing it only through `MainActivity` launches means these checks pay the
   full instrumented-test cost (emulator, ~30-60s per class) for what a JVM unit test would
   verify in milliseconds, and — per Issue 1 — over a third of these classes currently don't even
   run in CI.

## Mutation Check Results

One targeted mutation per file, applied to the source under test, run via
`./gradlew :app:testDebugUnitTest --tests`, then reverted.

| Test file | Mutation | Result |
|---|---|---|
| `PointFormulaTest` | `TaskEntity.computeAutoPoints()` bonus threshold `== 5` → `== 4` | **Not caught** — 9/9 passed. See Issue 2: this file tests a different copy of the formula. |
| `GatekeeperTest` | `canClaim`'s `mandatoryTasks.all { }` → `.any { }` | Caught — 3/7 failed |
| `RewardProgressTest` | `loggableTasks`'s repeatable-OR-not-yet-logged `\|\|` → `&&` | Caught — 3/15 failed |
| `DragReorderTest` | `targetIndex`'s dragged-item exclusion `!=` → `==` | Caught — 5/9 failed |
| `JsonImportValidationTest` | Schema-key check `earnItKeys.none { }` → `.any { }` | Caught — 7/8 failed |
| `NudgeDeciderTest` | First-threshold boundary `idleMs >= FIRST_THRESHOLD_MS` → `>` | Caught — 1/10 failed (the exact-boundary case) |
| `WidgetActionButtonTest` | `when` priority: `hasTasks` checked before `canClaim` | Caught — 1/6 failed |
| `ImportDedupTest` | Removed `.lowercase()` from the existing-names dedup set | Caught — 4/7 failed |

7 of 8 mutations were caught with a clear failure. The miss is a real gap (Issue 2), not a
mutation-design artifact — confirmed by tracing every call site of both formula copies.

## Spec Cross-Reference Notes

Core mechanics checked against `EARNIT_SPEC.md`: the auto-point formula, `canClaim` gating
("points ≥ cost AND every mandatory task logged since last claim"), `isRepeatable` semantics,
and import merge (`IGNORE` conflict strategy) / replace (`clearAllTables` then re-insert)
semantics. All match their corresponding test assertions (`GatekeeperTest`, `RewardProgressTest`,
`ExportImportTest`) precisely — no drift found.

One nuance ties back to Issue 2: `EARNIT_SPEC.md`'s Auto-Point Formula section documents the
formula as a single piece of logic, with the literal Kotlin expression quoted in the spec text.
It doesn't note that this expression exists in two independent source locations with asymmetric
test coverage — worth a one-line addition when the formula duplication is resolved.

No cascade-delete section exists in the spec beyond the Clean Up screen's user-facing
description; FK cascade behavior is already tracked as an open item in TESTING.md's Deferrals
section (real-Room verification not yet written) rather than a spec-vs-test mismatch.

## Work, Grouped by Branch

### chore/qa-audit (done)

Ran the audit; fixed TESTING.md's count drift and bug-history narration (Issues 3, 4),
CLEANUP_LOG.md's entry order (Issue 5), and DEV_PLAYBOOK.md's dangling Pass references (Issue 6).

### fix/duplicate-point-formula (follow-up, not started)

Deliverable: a single source of truth for the auto-point formula (Issue 2).

Steps:
1. Have `TaskEntity.computeAutoPoints()` delegate to (or be replaced by a call to)
   `EarnItRepository.computeAutoPoints()`, or vice versa — eliminate the duplicate rather than
   keeping both in sync by convention.
2. Add a direct boundary test for whichever copy remains canonical, if `PointFormulaTest`
   doesn't already cover it once the duplication is removed.

Tests: `PointFormulaTest` extended or retargeted so the formula actually used at log time has
direct boundary coverage.

### refactor/extract-field-validation-helpers (follow-up, not started)

Deliverable: character-cap, digit-only, and task-link-uncheck logic (Issue 9) as pure functions
with direct unit tests, matching the existing `DragReorder`/`PugslyGesture`/`WidgetActionButton`
precedent of extracting Compose-adjacent logic out of composables.

Steps:
1. Extract a shared `acceptWithinLimit(current: String, incoming: String, max: Int): String`
   (or equivalent) and wire it into the 8 character-cap call sites.
2. Extract a shared `String.digitsOnly(): String` and wire it into the 2 digit-filter call sites.
3. Extract the task-link uncheck-reset transformation into one shared function used by both
   `RewardEditScreen.kt` and `TaskEditScreen.kt` instead of two independent inline copies.
4. Add direct unit tests for all three; trim `MaxLengthUiTest` and the digit-filter/toggle-reset
   UI tests to a smaller set that verifies wiring only (one happy path, one boundary case per
   field), not the full matrix — the boundary matrix moves to the new unit tests.

Tests: 3 new unit test files/additions; existing UI tests trimmed, not removed.

### fix/ci-instrumented-test-matrix (follow-up, not started)

Deliverable: `instrumented-tests.yml` covers all 29 test classes.

Steps:
1. Add the 11 missing classes to the repository-utility/ui shards, rebalancing for runtime.
2. Add a CLEANUP_RULES.md Tests checklist item: confirm new instrumented test classes are
   added to the CI matrix.
3. Verify a full CI run passes with the expanded matrix.

Tests: no new test files; existing 11 files gain CI execution.

### refactor/ui-test-helpers (follow-up, not started)

Deliverable: shared Compose UI test flow-helper module, matching the existing
`CancelDismissAssertions.kt` precedent.

Steps:
1. Extract `createTask(name)`, `createReward(name)`, `waitForTaskDetail()`,
   `waitForRewardDetail()` into a new `UiTestActions.kt`.
2. Migrate the ~14-18 files with inline duplicates, including `DragReorderUiTest`'s private
   copies.
3. Confirm no behavior change — same assertions, same coverage, less duplicated setup code.

Tests: no new test cases; existing tests refactored to share helpers.

### fix/reward-edit-persistence-assertion (follow-up, not started)

Deliverable: `RewardEditScreenUiTest.editExistingReward_updatesFieldsAndPersists` asserts all
four fields it edits (Issue 8).

Steps:
1. Add assertions for the updated cost ("25") and description ("A well-earned break") to match
   `TaskEditScreenUiTest.editExistingTask_updatesFieldsAndPersists`'s pattern.

Tests: one existing test strengthened, no new test files.
