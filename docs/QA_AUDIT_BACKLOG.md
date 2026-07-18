# EarnIt — QA Audit Backlog

Tracks findings from the most recent QA audit (see [QA_AUDIT_RULES.md](QA_AUDIT_RULES.md) for
the procedure). When a listed follow-up branch lands, don't delete its entry — condense it:
shrink its Issues Found entry to one sentence stating what it was and that it's fixed (keep the
number, so it stays findable), and replace its Work Item's Steps with a short, dry summary of
what was actually done, marked `(done)`.

## Current State

173 unit tests (`app/src/test/`), 107 instrumented tests (`app/src/androidTest/`), grown
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

1. **CI matrix omitted ~40% of instrumented tests** (two shards hardcoded 18 of 29 classes;
   11 classes never ran on push/PR). Fixed on `fix/ci-instrumented-test-matrix`.

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

9. **Field-validation logic was inline in composables, not pure functions.** Fixed on
   `refactor/extract-field-validation-helpers`.

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

### refactor/extract-field-validation-helpers (done)

Extracted `acceptWithinLimit`/`digitsOnly` (`FieldValidation.kt`) and
`TaskEditState.withIncludedSetTo` (`SharedDialogs.kt`), matching the existing
`DragReorder`/`PugslyGesture`/`WidgetActionButton` precedent, and wired all 9 character-cap
sites, 2 digit-filter sites, and 3 uncheck-reset sites to them. The audit had named 8
character-cap sites; a 9th (`WidgetTaskLogActivity.kt`'s note field) turned out to use the
identical pattern and was included after confirming with the user. Added `FieldValidationTest`
(9 unit tests) covering the boundary cases directly. `MaxLengthUiTest` and the digit-filter/
toggle-reset UI tests were left as-is on review — each already asserted one happy path + one
boundary per field with no "full matrix," so there was nothing to trim.

Tests: `FieldValidationTest` (new, 9 cases); existing UI tests unchanged.

### fix/ci-instrumented-test-matrix (done)

Replaced the hardcoded per-shard class lists with a tagging convention: every androidTest class
now carries a required layer annotation (`@RepositoryTest`/`@UtilityTest`/`@UiTest`, in
`com.earnit.app.tags`) plus at least one optional tag (`@Smoke` or a feature area). CI filters by
annotation instead of an explicit class list, so a new test class runs automatically once tagged.
`checkInstrumentedTestTags` (`./gradlew check`) fails the build if a class is missing either tag —
see TESTING.md's Tagging convention and the CLEANUP_RULES.md Tests checklist item.

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
