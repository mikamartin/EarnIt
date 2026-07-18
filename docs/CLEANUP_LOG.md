# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 48 — `test/double-tap-logging` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s "Rapid double-tap logging" item. The backlog entry's premise was that `TESTING.md`'s existing framing (DAO writes serialized, button re-evaluates after each Flow emission) didn't actually rule out two `logCompletion` calls for the same non-repeatable task both succeeding. Confirmed with the user: write the concurrent repository test first and let its result decide whether a guard is warranted, rather than assuming either way. It confirmed the gap — `logCompletion` had no loggability check at all, only an archived-reward check, so two concurrent calls each inserted a log. Confirmed with the user to add a repository-level guard, mirroring `claimReward`'s and `logCompletion`'s existing archived-reward pattern (`fix/archived-reward-log-guard`, Pass 47).

#### Duplication ✅ (checked, none found)
- New DAO query (`getActiveLogCount`) is the only new query added; no existing query already answered "is there an active log for this task+reward."

#### Decoupling ✅ (n/a)
- Change is contained entirely within `EarnItRepository` and `Daos.kt`; no ViewModel or UI changes.

#### Complexity & Pattern Health ✅ (checked)
- Guard is a single early-return inside the existing `logCompletion`, now wrapped in `database.withTransaction { }` (same pattern as `claimReward`) so the check-then-insert is atomic across concurrent callers rather than just narrowing the race window.

#### Dead Code & Hygiene ✅
- ktlint clean.
- `git status` clean apart from the intended files: `EarnItRepository.kt`, `Daos.kt`, `RepositoryTestBase.kt`, `ConcurrentLogCompletionTest.kt`, `TESTING.md`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅ (n/a)
- `ConcurrentLogCompletionTest` follows the existing `*Test.kt` convention and sits flat in `app/src/androidTest/java/com/earnit/app`, matching every other repository-tier test's placement.

#### Hardcoded Values ✅ (n/a)
- None introduced; test literals ("Morning Run", "Coffee Treat") match `HappyPathTest`'s existing fixture values.

#### Accessibility ✅ (n/a)
- No UI touched.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (checked, no changes needed)
- `EARNIT_SPEC.md`'s task field table already documents `Repeatable` as "If true, can be logged multiple times" — this pass makes actual behavior match that contract more robustly; no spec text was inaccurate.

#### Tests ✅ (1 new file, 1 existing file updated for a mock-strictness ripple, docs updated)
- New `ConcurrentLogCompletionTest` run alone first with the guard absent to confirm the race was real (2 logs written), then again after adding the guard (1 log written) — both runs on a connected emulator. Targeted subset (`HappyPathTest`, `LogAgainstArchivedRewardTest`, `StartOverTest`, `ExportImportTest`, `ClearCascadeTest`, `ConcurrentLogCompletionTest`) run together: 21/21 pass. Full instrumented suite: 105/105 pass, no regressions.
- Adding the `rewardTaskDao.getTaskRefsForReward` call inside `logCompletion` broke 5 pre-existing `LogAttributionTest` unit tests (strict mock, unstubbed call). Fixed by adding a default `coEvery { rewardTaskDao.getTaskRefsForReward(any()) } returns emptyList()` to `RepositoryTestBase`'s init block, alongside the existing `withTransaction` stub, since this call is now on the path of every `logCompletion` invocation across the suite rather than specific to one test class.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required on its own — covered anyway by the full `connectedDebugAndroidTest` run.
- `./gradlew ktlintCheck`, `test`, and `assembleDebug` all pass, run sequentially per `CLAUDE.md`.
- `TESTING.md`: new `ConcurrentLogCompletionTest` row added to the Instrumented Tests table (Repository layer); the "Rapid double-tap logging" entry moved from "Not Covered" (now empty, header removed) to a new "Covered" entry under Edge Cases describing the transaction-scoped guard.
- `CLEANUP_BACKLOG.md`: this item removed now that it's actioned.

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
