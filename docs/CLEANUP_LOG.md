# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 50 — `refactor/extract-field-validation-helpers` branch

Actions [QA_AUDIT_BACKLOG.md](QA_AUDIT_BACKLOG.md)'s Issue 9. Character-cap truncation, digit-only filtering, and the task-link uncheck-reset transform were each duplicated inline across `onValueChange`/`onClick` blocks in multiple composables, so they could only be exercised through full `MainActivity` + Hilt + Compose instrumented tests. Extracted `acceptWithinLimit`/`digitsOnly` (`FieldValidation.kt`, mirroring the `DragReorder`/`WidgetActionButton` precedent) and `TaskEditState.withIncludedSetTo` (`SharedDialogs.kt`, next to the data class it operates on), then wired every call site to them. The audit named 8 character-cap sites; reading the current code found a 9th (`WidgetTaskLogActivity.kt`'s note field, same inline shape) it had missed — confirmed with the user to include it rather than leave one duplicate standing.

#### Duplication ✅ — found and fixed
Grepped every `it.length <=`/`filter { c -> c.isDigit() }` site across `app/src/main` before and after: 9 character-cap and 2 digit-filter sites now all call the shared functions; the 3 task-link uncheck-reset `.copy(...)` blocks (`RewardEditScreen.kt`, two in `TaskEditScreen.kt`) now all call `withIncludedSetTo`.

#### Decoupling ✅ (n/a)
All three extractions stayed in the `ui` package — `acceptWithinLimit`/`digitsOnly` are plain `String` transforms with no Compose or business-logic coupling, and `withIncludedSetTo` operates on `TaskEditState`, a UI-layer state class already living in `SharedDialogs.kt`. Nothing here belongs in the ViewModel or Repository.

#### Complexity & Pattern Health ✅ (checked)
None of the three extractions are single-caller: `acceptWithinLimit` has 9 call sites, `digitsOnly` has 2, `withIncludedSetTo` has 3 — the "only one caller" flag doesn't apply.

#### Dead Code & Hygiene ✅
`./gradlew ktlintCheck` clean. `git status` clean apart from the intended files: `FieldValidation.kt` (new), `FieldValidationTest.kt` (new), `RewardEditScreen.kt`, `TaskEditScreen.kt`, `SettingsScreen.kt`, `SharedDialogs.kt`, `TasksScreen.kt`, `WidgetConfigActivity.kt`, `WidgetTaskLogActivity.kt`, `QA_AUDIT_BACKLOG.md`, `TESTING.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅
`FieldValidation.kt`/`FieldValidationTest.kt` follow the `DragReorder`/`DragReorderTest` flat-`ui`-package precedent.

#### Hardcoded Values ✅ (n/a)
No new magic numbers — all call sites reuse the existing `*_MAX_CHARS` constants (`AppHelpers.kt`).

#### Accessibility ✅ (n/a)
No UI touched — same fields, same behavior, different implementation underneath.

#### Deprecated APIs ✅
None touched.

#### Spec Review ✅ (checked, no changes needed)
Grepped `EARNIT_SPEC.md` for character-limit/validation terms — it doesn't document these implementation details (field caps, digit filtering), so there's no contract to reconcile.

#### Tests ✅ (1 new file, docs updated)
- `FieldValidationTest` (9): `acceptWithinLimit` under/at/over the boundary plus a same-length replacement at the cap; `digitsOnly` on mixed/all-digit/no-digit input; `withIncludedSetTo` resetting both flags on uncheck regardless of prior state vs. leaving them untouched on check.
- Reviewed `MaxLengthUiTest` and the digit-filter/toggle-reset cases in `RewardEditScreenUiTest`/`TaskEditScreenUiTest` against the backlog's "trim to wiring-only" step — each already asserted exactly one happy path + one boundary per field with no full matrix, so nothing was trimmed; left them as regression coverage for the wiring.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`. Manual on-device spot check deferred to the PR description per the user's request, not run this pass.
- `TESTING.md`: new `FieldValidationTest` row and a Covered edge-case entry; unit count 165+ → 175+ (actual count 173, rounded per `CLEANUP_RULES.md`).
- `QA_AUDIT_BACKLOG.md`: Issue 9 condensed to one sentence; this branch's Work Item replaced with a dry `(done)` summary, including the 8→9 site-count correction.

---

### Pass 51 — `refactor/ui-test-helpers` branch

Actions [QA_AUDIT_BACKLOG.md](QA_AUDIT_BACKLOG.md)'s Issue 7. "Create a task," "create a reward," and "wait for Task/Reward Detail" were each duplicated inline across roughly a third of the Compose UI test files, including two private per-file copies (`TaskEditScreenUiTest`, `SharedDialogsCancelUiTest`, `RewardProgressBarUiTest`, `RewardEditScreenUiTest` each reimplemented their own `waitForTaskDetail()`/`waitForRewardDetail()`; `DragReorderUiTest` reimplemented `addTask`/`addReward` outright). Extracted `createTask(name)`, `createReward(name, cost)`, `waitForTaskDetail()`, `waitForRewardDetail()` into `UiTestActions.kt` (mirroring the `CancelDismissAssertions.kt` precedent) and migrated the 12 files whose blocks matched those base-case signatures. Call sites needing extra fields, a different entry point (e.g. a reward form's own "Create your own" task dialog), or no SAVE/wait at all were left inline rather than growing the helpers' signatures for a single caller — confirmed by re-grepping every remaining `"New Task"`/`"New Reward"` site after migration and checking each one actually diverges from the base case (a prior click-before-type field-focus click, a form never saved, or a second form reopened mid-test).

#### Duplication ✅ — found and fixed
Re-grepped `onNodeWithContentDescription("New Task"/"New Reward"/Strings.NEW_TASK_DESC/Strings.NEW_REWARD_DESC)` post-migration: every remaining inline hit is a genuine variant (second form reopened to check a duplicate-name error, click-before-type sequence in `RewardAllTasksLoggedHintUiTest`/`UiHappyPathTest`, no-SAVE cap tests, task/reward linked before its own SAVE), not a missed absorption opportunity.

#### Decoupling ✅ (n/a)
Test-only change — no ViewModel, Repository, or Dao touched.

#### Complexity & Pattern Health ✅ (checked)
None of the four new helpers are single-caller: `createTask`/`createReward` each have 16 call sites, `waitForTaskDetail` 18, `waitForRewardDetail` 12 (counted post-migration) — the "only one caller" flag doesn't apply.

#### Dead Code & Hygiene ✅
Removed the now-unused import (`onAllNodesWithText`, `performTextClearance`, and/or `performTextInput`, per file) from each of the 12 migrated files as its inline block was replaced; `ktlintCheck` (which fails on unused imports) passed clean. `git status` clean apart from the intended files.

#### Naming Consistency ✅
`UiTestActions.kt` sits flat in `app/src/androidTest/java/com/earnit/app`, matching `CancelDismissAssertions.kt`'s placement and extension-function style (`ComposeTestRule` receiver). Grepped for existing `createTask`/`createReward`/`waitForTaskDetail`/`waitForRewardDetail` function definitions before adding these — only pre-existing test method names containing those words as substrings, no actual shadowing.

#### Hardcoded Values ✅ (n/a)
`waitUntil(timeoutMillis = 5_000)` matches the exact value already used at every call site being replaced — not a new magic number.

#### Accessibility ✅ (n/a)
No UI touched.

#### Deprecated APIs ✅
None touched.

#### Spec Review ✅ (checked, no changes needed)
Grepped `EARNIT_SPEC.md` for `UiTestActions`/`createTask`/`createReward` — no hits; the spec doesn't document test infrastructure, so there's no contract to reconcile.

#### Tests ✅ (0 new test cases, 12 files refactored, docs updated)
- No test method signatures were added, removed, or renamed — `git diff` on `app/src/androidTest` shows no changed `fun ...()` test-declaration line, only bodies. Total `@Test` count unchanged at 107, matching `TESTING.md`'s existing figure.
- Ran the full 55-test subset covering all 12 migrated files (`./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...`) on a connected emulator: 55/55 pass, no regressions from the refactor.
- `checkInstrumentedTestTags` passed — `UiTestActions.kt` needs no layer/optional tag, same as the untagged `CancelDismissAssertions.kt`/`TestStateReset.kt` precedent (neither is a `@RunWith` test class).
- No `AppModule`/`TestAppModule`/`@Inject` changes; ran `assembleDebugAndroidTest` anyway since the change touches androidTest broadly — passed, full Hilt graph compiles.
- `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest` all pass sequentially per `CLAUDE.md`.
- `TESTING.md`: new "Shared flow helpers" paragraph describing `UiTestActions.kt`'s scope and what stays inline; no count changes needed (test count unchanged).
- `QA_AUDIT_BACKLOG.md`: Issue 7 condensed to one sentence; this branch's Work Item replaced with a dry `(done)` summary.

---

### Pass 52 — `fix/duplicate-point-formula` branch

The auto-point formula was implemented twice: `TaskEntity.computeAutoPoints()` (the copy actually used to award points, via `effectivePoints()`) and `EarnItRepository.computeAutoPoints(time, difficulty, preparation)` (used only for the live slider preview in `TaskEditScreen`). `PointFormulaTest`'s boundary assertions all exercised the repository copy, so the formula that actually awards points had no direct boundary coverage — confirmed by a prior QA mutation check (mutating the `== 5` bonus condition on the entity's copy left the suite green). Made `TaskEntity`'s companion object the single source of truth: added a pure `TaskEntity.computeAutoPoints(time, difficulty, preparation)`, with the entity's zero-arg instance method delegating to it. Removed `EarnItRepository`'s copy; `EarnItViewModel.computeAutoPoints()` now calls `TaskEntity`'s companion function directly.

#### Duplication ✅ — found and fixed
This pass's entire purpose: eliminated the second implementation rather than keeping both in sync by convention. Grepped `computeAutoPoints` across `app/src/main` post-change: exactly one formula body remains (`TaskEntity`'s companion function); every other call site delegates to it.

#### Decoupling ✅ (checked)
The canonical formula lives in `TaskEntity`'s companion object (data/entity layer, no dependency on `EarnItRepository`) rather than the reverse — a repository owning a pure math formula, or an entity depending on the repository layer, would have been backwards.

#### Complexity & Pattern Health ✅ (checked)
The companion function has two callers (the instance method, `EarnItViewModel`) — not single-caller, so the extraction earns its keep.

#### Dead Code & Hygiene ✅
`./gradlew ktlintCheck` clean (fails on unused imports; confirmed no stale `mockk`/`EarnItRepository` imports left in `PointFormulaTest.kt`). `git status` clean apart from the intended files.

#### Naming Consistency ✅ (n/a)
No new files.

#### Hardcoded Values / Accessibility / Deprecated APIs ✅ (n/a)
No UI or magic numbers touched; formula constants unchanged.

#### Spec Review ✅
`EARNIT_SPEC.md`'s Auto-Point Formula section now names `TaskEntity.computeAutoPoints(...)` as the single source of truth, closing a gap where the spec didn't note the formula previously existed in two places.

#### Tests ✅ (7 retargeted, 1 new case)
- `PointFormulaTest`'s 7 boundary assertions now call `TaskEntity.computeAutoPoints(...)` directly — no more mocked `EarnItRepository`, and the copy under test is now the one that actually awards points.
- Added a new case exercising the entity's zero-arg instance method (the exact path `logCompletion` and every points-display site use) at the dimension-5 bonus boundary.
- `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest` (run since `EarnItRepository` changed, to validate the Hilt graph), `assembleDebug` all pass sequentially per `CLAUDE.md`.
- `TESTING.md`: `PointFormulaTest` row count 9 → 10 (exact, per-file); aggregate unit count unchanged (174 actual still rounds to the existing 175+ figure).
- `QA_AUDIT_BACKLOG.md`: every item was resolved or already fixed on its own branch, so the backlog was cleared back to just the intro convention, ready for the next audit pass.
