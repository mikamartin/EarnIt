# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 49 — `test/drag-reorder-coverage` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s "Drag-to-reorder gesture on Home has no automated coverage" item. Extracted the hover-target/list-move math shared by Home's and Tasks' drag gestures into `DragReorder` (mirroring `WidgetActionButton`/`PugslyGesture`) — grepping for `detectDragGesturesAfterLongPress` beyond the file the backlog named surfaced an identical inlined copy in `TasksScreen.kt`, so both screens were wired to the shared object, closing real duplication too.

Manual on-device testing (requested before trusting the `add`/`removeAt` → `clear`/`addAll` mutation-pattern change) found Home's drag didn't move cards at all — a pre-existing bug, not something this refactor introduced: `draggingIndex` was passed into `homeRewardListItems` as a plain `Int`, frozen by value inside the `pointerInput` closure, so `onDrag` always read a stale index. Fixed by changing it to a `() -> Int` getter. Also found, contrary to the backlog's own assumption, that Compose's `performTouchInput` (with `advanceEventTime` to clear the long-press threshold before moving) *can* drive the real gesture reliably, so `DragReorderUiTest` now exercises it end-to-end on both screens instead of leaving it manual-only.

#### Duplication ✅ — found and fixed
Grepped `detectDragGesturesAfterLongPress` across `app/src/main` before and after: found `TasksScreen.kt`'s inlined copy of the reorder math pre-change; confirmed both screens delegate to `DragReorder` post-change (the two remaining call sites are the necessarily screen-specific gesture *wiring*, not the math).

#### Decoupling ✅
Reorder math moved out of two composables into a plain `ui`-package object — kept out of the ViewModel since it's Compose-layout-derived geometry (`LazyListItemInfo` offsets), matching the `PugslyGesture` precedent.

#### Complexity & Pattern Health ✅
`DragReorder` is a two-function object with two real call sites (clears the "only one caller" flag). Checked composable length: `homeRewardListItems` unchanged (~105 lines); `TasksScreen` itself is ~272 lines but pre-existing and not grown by this diff — out of scope for a test-coverage pass.

#### Dead Code & Hygiene ✅
Force-recompiled all three source sets (`compileDebugKotlin compileDebugUnitTestKotlin compileDebugAndroidTestKotlin --rerun`) and grepped for warnings — none from this diff. ktlint clean. `git status` clean apart from the intended files.

#### Naming Consistency ✅
`DragReorder.kt`/`DragReorderTest.kt` follow the `PugslyGesture`/`PugslyGestureTest` flat-`ui`-package precedent. `DragReorderUiTest.kt`'s package (`com.earnit.app`, flat) and `*UiTest.kt` suffix checked against every other instrumented UI test file — matches. Its literal UI strings ("Reward name", "New Task", etc.) match the *majority* existing convention (`SaveNavigationUiTest` and others use literals too), though the suite is genuinely split — some newer files reference `Strings.kt` constants instead. Pre-existing inconsistency, not introduced here; left as-is rather than doing an unrelated suite-wide pass.

#### Hardcoded Values / Accessibility / Deprecated APIs ✅ — n/a
No UI or API surface touched beyond delegating existing math and the closure fix.

#### Spec Review ✅
Grepped `EARNIT_SPEC.md` for drag/reorder/sort-order terms — already describes both screens as drag-reorderable; this pass makes actual behavior match that description rather than changing it.

#### Tests ✅ (3 new files, 1 real regression-tested bug fix)
- `DragReorderTest` (9): pure hover-math and list-move unit tests.
- `DragReorderUiTest` (2): drives the real long-press-drag via `performTouchInput` on both screens, asserting actual on-screen order. Verified this test fails against the pre-fix code with the same symptom found manually, and passes with the fix — confirmed by temporarily stashing the fix and re-running.
- Ran on two connected devices (API 34 emulator, API 36 physical device) — 2/2 pass on both.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially. No `AppModule`/`TestAppModule`/`@Inject` changes.
- `TESTING.md`: new `DragReorderTest`/`DragReorderUiTest` rows and a Covered edge-case entry describing the bug; unit count 150+ → 165+ (actual count 164, rounded per `CLEANUP_RULES.md`); instrumented/UI pyramid counts (107/72 actual) still round to the existing ~105/~70 figures, unchanged.
- `CLEANUP_BACKLOG.md`: this item removed and file deleted per its own disposal note.

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
