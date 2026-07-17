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
- Ran on two connected devices (Pixel 6 API 34 emulator, Pixel 8 API 36) — 2/2 pass on both.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially. No `AppModule`/`TestAppModule`/`@Inject` changes.
- `TESTING.md`: new `DragReorderTest`/`DragReorderUiTest` rows and a Covered edge-case entry describing the bug; unit count 150+ → 165+ (actual count 164, rounded per `CLEANUP_RULES.md`); instrumented/UI pyramid counts (107/72 actual) still round to the existing ~105/~70 figures, unchanged.
- `CLEANUP_BACKLOG.md`: this item removed and file deleted per its own disposal note.

---

### Pass 47 — `fix/archived-reward-log-guard` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s "Logging against an archived reward has no repository guard, and no test" item. `EarnItRepository.logCompletion` inserted unconditionally with no check on the reward's archived state. Research during planning confirmed the realistic trigger: a stale UI surface (e.g. a reward claimed from one screen while its LOG button is still visible elsewhere) calling `logCompletion` after the reward is archived, producing an orphaned log with no `historyEntryId`. Confirmed with the user to add a repository-level guard (not just a documentation test): `logCompletion` now fetches the reward and returns early if it's missing or archived, silently skipping the insert rather than surfacing an error — matches the existing precedent in `claimReward` (`EarnItRepository.kt:154`, no-ops when the reward is missing) and needs no signature change since neither call site (`EarnItViewModel.logTask`, `WidgetTaskLogActivity`) inspects a result today.

#### Duplication ✅ (checked, none found)
- The new guard reuses `rewardDao.getReward`, already called identically in `claimReward` — no new DAO query added.

#### Decoupling ✅ (n/a)
- Change is contained entirely within `EarnItRepository`; no ViewModel or UI changes.

#### Complexity & Pattern Health ✅ (checked)
- Two-line early-return guard, no new branching structure or abstraction.

#### Dead Code & Hygiene ✅
- ktlint clean.
- `git status` clean apart from the intended files: `EarnItRepository.kt`, `LogAttributionTest.kt`, `LogAgainstArchivedRewardTest.kt`, `TESTING.md`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅ (n/a)
- No new files besides the test, which follows the existing `*Test.kt` convention and sits flat in `app/src/androidTest/java/com/earnit/app`, matching every other repository-tier test's placement.

#### Hardcoded Values ✅ (n/a)
- None introduced; test literals ("Morning Run", "Coffee Treat") match `HappyPathTest`'s existing fixture values.

#### Accessibility ✅ (n/a)
- No UI touched.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (checked, no changes needed)
- Grepped `EARNIT_SPEC.md` for `logCompletion`/archived-reward terms — this is an internal robustness guard, not a documented behavior contract, so no drift to reconcile.

#### Tests ✅ (1 new file, 1 existing file updated for a mock-strictness ripple, docs updated)
- New `LogAgainstArchivedRewardTest.logCompletion_onArchivedReward_isSkipped` run alone on a connected API 34 emulator first: pass. Full instrumented suite re-run on the same emulator: 104/104 pass. One `RewardLimitUiTest` failure (unrelated to this change) plus a process crash occurred on the first full run; re-run alone it passed cleanly, and a second full run completed 104/104 clean — confirming emulator flakiness under a long continuous run, not a regression, consistent with the pattern noted in Pass 46.
- Fixing the repository guard broke 5 pre-existing `LogAttributionTest` unit tests: `RepositoryTestBase`'s `rewardDao` mock is strict (not relaxed), and those tests called `logCompletion` without stubbing the new `rewardDao.getReward` call, so MockK threw. Fixed by adding `coEvery { rewardDao.getReward(any()) } returns RewardEntity(...)` to each, following the same explicit-per-test stubbing convention already used in `RepositoryBehaviourTest`/`ClaimRewardStartOverTest` rather than changing the shared base.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required on its own — covered anyway by the full `connectedDebugAndroidTest` run.
- `./gradlew ktlintCheck`, `test`, and `assembleDebug` all pass, run sequentially per `CLAUDE.md`.
- `TESTING.md`: new `LogAgainstArchivedRewardTest` row added to the Instrumented Tests table (Repository layer); the "Logging against an archived reward" entry moved from "Not Covered" to a new "Covered" entry under Edge Cases describing the guard and why it's a silent skip rather than a surfaced error.
- `CLEANUP_BACKLOG.md`: this item removed now that it's actioned.

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
