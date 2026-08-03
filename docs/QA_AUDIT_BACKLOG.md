# EarnIt — QA Audit Backlog

Tracks findings from the most recent QA audit (see [QA_AUDIT_RULES.md](QA_AUDIT_RULES.md) for
the procedure). When a listed follow-up branch lands, don't delete its entry — condense it:
shrink its Issues Found entry to one sentence stating what it was and that it's fixed (keep the
number, so it stays findable), and replace its Work Item's Steps with a short, dry summary of
what was actually done, marked `(done)`.

## What's Working

- CI test-execution: unit tests (`./gradlew test`, no filter) and the two androidTest shards
  (`annotation=UiTest` / `notAnnotation=UiTest`) are logically exhaustive — no test class is
  silently skipped by the shard split.
- Structural assertion review: 3 of the 7 sampled files (`SharedDialogsCancelUiTest`,
  `LogForRewardDialogUiTest`, `RewardAllTasksLoggedHintUiTest`) had zero action-chaining or
  under-assertion violations — each test asserts exactly what its name claims.
- Spec cross-reference: the auto-point formula, gatekeeper `canClaim` logic, per-reward log
  attribution, and the claim/start-over flow all matched their spec sections exactly (the
  point-formula and gatekeeper boundary conditions are additionally confirmed by this pass's
  mutation checks, below).
- `CLEANUP_LOG.md` retention and ordering (3 most recent passes, oldest-first) is correct.

## Issues Found

All findings below were small enough to fix directly on this audit branch — see
[QA_AUDIT_RULES.md](QA_AUDIT_RULES.md) for when a finding instead becomes a separate proposed
branch; none did this pass.

1. **CI never ran `checkInstrumentedTestTags`** (done) — the layer-tag enforcement task was wired
   into Gradle's `check` task, but no workflow ever invoked `check` or the task directly, so a
   class with a missing/duplicate layer tag would never be caught by CI. Added an explicit step to
   `ci.yml`.
2. **Reward-progress-fraction formula duplicated in 4 places, untested** (done) — `HomeScreen.kt`,
   `RewardDetailScreen.kt`, `EarnItWidget.kt`, and `WidgetConfigActivity.kt` each independently
   computed `(points / cost).coerceIn(0f, 1f)`. Extracted to `RewardProgress.progressFraction`
   and updated all 4 call sites. Writing the test surfaced a real latent edge case: a zero-cost
   reward divides 0/0, producing `NaN` instead of a sensible "fully progressed" value — fixed to
   return `1f` for zero-cost rewards, matching `canClaim`'s treatment of zero-cost as trivially
   met. Added 3 unit tests to `RewardProgressTest`.
3. **Cap-then-conditionally-reset-sibling-state pattern duplicated, untested** (done) —
   `SettingsScreen.kt`'s nickname field and `TaskEditScreen.kt`'s group field each inline the same
   shape (cap via `acceptWithinLimit`, then conditionally reset unrelated state on an accepted
   edit). Extracted to `nicknameFieldEdit`/`taskGroupFieldEdit` in `FieldValidation.kt`, following
   the existing `DragReorder`/`PugslyGesture`/`WidgetActionButton` extraction precedent. Added 5
   unit tests to `FieldValidationTest`.
4. **"Abandon new entity" UI flow duplicated across two test files** (done) —
   `RewardEditScreenUiTest` and `TaskEditScreenUiTest` each hand-rolled the same "open form → type
   name → CANCEL → assert discarded" flow. Added `cancelNewEntityAndAssertDiscarded(...)` to
   `UiTestActions.kt`; both tests now use it.
5. **4 action-chaining violations in the largest/newest instrumented tests** (done) —
   `editExistingReward_updatesFieldsAndPersists`, `editExistingTask_updatesFieldsAndPersists`,
   two tests in `SettingsScreenUiTest` (`maxRewardCount_editedInSettingsSlider_enforcesCapOnHomeFab`,
   `selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate`), and
   `exportImportReplace_preservesHistoryEntriesWithArchivedLogs` each chained multiple
   state-changing steps with no assertion checkpoint between them. Added intermediate assertions
   in all 5 methods so a failure now points at the exact step that broke. No under-assertion
   violations were found.
6. **`TESTING.md`'s `DeleteCascadeTest` description was backwards** (done) — it claimed
   `deleteTask`/`deleteReward` manually clear cross-refs before deleting, when the code (and the
   test's own docstring) does the opposite: cross-ref cleanup is left to the `RewardTaskCrossRef`
   FK cascade, and `DeleteCascadeTest` explicitly verifies no manual clearing call happens. Fixed
   the table entry and the matching Edge Case narrative.
7. **`EARNIT_SPEC.md`'s import-validation order didn't match the (correct) implementation** (done)
   — the spec stated JSON-validity is checked before the schema check; the code deliberately does
   the opposite, because Moshi's generated adapter would otherwise silently accept a
   syntactically-valid-but-wrong-shape JSON body (e.g. an unrelated object) as an all-empty
   export rather than fail. Reordering the code to match the spec would have broken the existing
   `fromJson with JSON array throws WrongSchemaException` test, so the spec was corrected instead.
8. **Two mutation-caught tests passed only via a MockK strict-mock crash, not an assertion**
   (done) — `ClaimRewardStartOverTest`'s two `startOver=true` tests and
   `ImportDedupTest`'s trim-whitespace test caught their mutations only because an unstubbed mock
   call threw, not because of an explicit check. Fragile if the mocks are ever relaxed. Added
   explicit stubs and `coVerify(exactly = 0)` assertions to all three.
9. **Dangling `CLEANUP_LOG.md` "Pass N" cross-references outside the retained window** (done) —
   `TESTING.md` referenced Pass 49 (four Edge Case entries narrated bug-discovery history instead
   of current behavior); `EarnItRepository.kt` and `TestDataSeeder.kt` each referenced Pass 21 in
   a code comment. All five reworded to state current behavior/rationale directly.
10. **Aggregate test-count figures had drifted** (done) — `TESTING.md`/`EARNIT_SPEC.md` claimed
    "175+" unit tests (actual was 174 before this pass's additions, now 182) and "~105"/"~60"
    instrumented tests (actual is 108). Recomputed and fixed everywhere they're stated.
11. **`TaskLibraryScreenUiTest`'s back-press dismiss test was flaky in this environment** (done)
    — `Espresso.pressBack()` dispatches a real system key event, which requires the emulator
    window to hold actual OS-level focus; it failed with `RootViewWithoutFocusException` during
    this pass's full-suite verification run and again in isolation. Replaced with
    `activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }`, which
    exercises the same "system back" code path without depending on real window focus.
    Re-verified passing, both in isolation and as part of a full clean 108/108 instrumented run.

## Mutation Check Results

All 10 mutations were caught. 8 were caught cleanly on the first attempt; the 2 flagged below were
caught only via a mock crash and were strengthened per Issue 8 above, then re-confirmed caught via
an explicit assertion failure.

| File under test | Mutation | Result |
|---|---|---|
| `PointFormulaTest` | Bonus threshold `== 5` → `== 4` | Caught (assertion) |
| `GatekeeperTest` | `canClaim` boundary `>=` → `>` | Caught (assertion) |
| `ClaimRewardStartOverTest` | Inverted `!startOver` archive guard | Caught (mock crash) → strengthened → re-confirmed caught (assertion) |
| `RepositoryBehaviourTest` | Swapped `isMandatory`/`isRepeatable` constructor args | Caught (assertion) |
| `ImportViewModelErrorTest` | Swapped `InvalidJson`/`WrongSchema` string mapping | Caught (assertion) |
| `ImportDedupTest` | Dropped `.trim()` before dedup check | Caught (mock crash) → strengthened → re-confirmed caught (assertion) |
| `LogAttributionTest` | Bypassed `effectivePoints()`, used raw `points` | Caught (assertion) |
| `DeleteCascadeTest` | Swapped call order in `deleteReward` | Caught (assertion) |
| `FieldValidationTest` (new `nicknameFieldEdit`) | Dropped the "accepted edit" guard condition | Caught (assertion) |
| `FieldValidationTest` (new `taskGroupFieldEdit`) | Inverted `isNotBlank()` → `isBlank()` | Caught (assertion) |

## Spec Cross-Reference Notes

| Mechanic | Result |
|---|---|
| Auto-point formula | Matches spec exactly; boundary confirmed by mutation check |
| Gatekeeper `canClaim` | Matches spec exactly; boundary confirmed by mutation check |
| Per-reward point pool / log attribution | Matches; "excess logs archived in full" is structurally guaranteed by the unfiltered `archiveLogsForReward` SQL, not separately tested — not a gap |
| Claiming flow (start over / archive) | Matches spec exactly |
| Delete/clear cascade semantics | No explicit spec-stated contract to check tests against — a spec-anchor gap, not a test defect |
| Import validation order | Mismatch found and fixed — see Issue 7 above |
