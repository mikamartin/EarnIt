# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 46 — `test/cancel-dismiss-coverage` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s "Cancel/dismiss buttons untested across the entire app" item: 15 distinct Cancel/dialog-dismiss surfaces across 8 files had zero dedicated cancel-path coverage — some dialogs were tested on their confirm path only; `CleanUpScreen` and `TaskLibraryScreen` had no test file at all. Research during planning found a real inventory of every surface (confirmed via grep, not assumption) before writing anything.

#### Duplication ✅ (1 shared helper extracted)
- New `CancelDismissAssertions.kt` — `cancelDialogAndAssertDismissed(dialogMarkerText, cancelText)` — removes the repeated "click Cancel, assert the dialog is gone" boilerplate across ~11 dialog tests. Deliberately left as a helper, not a forced one-shape-fits-all abstraction: each test still supplies its own setup and its own side-effect assertion (no task created, no log recorded, reward not archived), since those genuinely differ per dialog.
- `CleanUpScreenUiTest`'s private `assertCancelClearsNothing` helper mirrors the existing `waitForRewardDetail()`-style per-class private helper pattern (`RewardEditScreenUiTest`, `TaskEditScreenUiTest`) rather than promoting it to a shared file — its assertions (repository row counts) are specific to that screen.

#### Decoupling ✅ (n/a)
- Test-only files; no production code touched.

#### Complexity & Pattern Health ✅ (checked)
- `cancelDialogAndAssertDismissed` is a single ~10-line extension function on `SemanticsNodeInteractionsProvider` — no new interface or base class introduced.
- Confirmed via grep that `isDialog()`-based scoping was necessary, not incidental complexity: several dialogs render a Cancel button with the exact text "CANCEL" while an identically-labeled button from the screen behind them is still in the merged semantics tree (Compose dialogs don't dispose the underlying screen), so a plain `onNodeWithText("CANCEL")` would match two nodes. Validated this technique on-device with one test before rolling it out to the other ten.

#### Dead Code & Hygiene ✅ (checked)
- ktlint clean on every check, re-verified after all edits (including the debugging round below).
- Diagnostic code added while tracking down two real test bugs (`printToLog`, `onRoot`, a `Thread.sleep`) was removed before finalizing — none of it reached this state; confirmed via a final read of `TaskLibraryScreenUiTest.kt`.
- `git status` clean apart from the intended files: `CancelDismissAssertions.kt`, `CleanUpScreenUiTest.kt`, `TaskLibraryScreenUiTest.kt`, `SharedDialogsCancelUiTest.kt`, `RewardEditScreenUiTest.kt`, `TaskEditScreenUiTest.kt`, `SettingsScreenUiTest.kt`, `TESTING.md`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅ (checked)
- Listed every non-`*Test.kt` file already in `app/src/androidTest/java/com/earnit/app` (`HiltTestRunner.kt`, `RoomIntegrationBase.kt`, `TestStateReset.kt`) to confirm `CancelDismissAssertions.kt` follows the established precedent for a non-suffixed helper sitting flat in the same package, rather than inventing a new convention.

#### Hardcoded Values ✅ (n/a)
- No UI code touched; test literals ("Morning Run", "Walk Dog", etc.) match existing fixture naming used elsewhere in this suite.

#### Accessibility ✅ (n/a)
- No production UI touched.

#### Deprecated APIs ✅ (checked)
- Recompiled (`compileDebugAndroidTestKotlin`) and grepped the output for deprecation warnings — none from this diff.

#### Spec Review ✅ (checked, no changes needed)
- Grepped `EARNIT_SPEC.md` for Cancel/dismiss/Clean Up/Task Library terms — the existing entries (e.g. the Task Library row already describing "tasks with duplicate names are skipped and listed in a post-import dialog") already match current behavior. No behavior changed by this pass, only coverage added, so no drift to reconcile.

#### Tests ✅ (4 new files, 3 real bugs found and fixed on-device, docs updated)
- Three real bugs were found and fixed by actually running the new tests on a connected emulator, not just compiling them: (1) `CleanUpScreenUiTest`'s navigation to the Clean Up row needed `.performScrollTo()` — without it, the click missed and every assertion after failed confusingly two steps later; added an immediate post-navigation assertion so a future failure like this points at the right step. (2) The same screen's `DangerButton` uppercases its label at render time (`SettingsScreen.kt`) — `onNodeWithText` is case-sensitive by default, so the button was never found; fixed with `ignoreCase = true`. (3) `TaskLibraryScreenUiTest`'s "ADD 10 TASKS" button lives inside a `LazyColumn` below 10 task rows — composed (found by `waitUntil`) but not actually on-screen, so `performClick()` silently did nothing; fixed with `.performScrollTo()`, confirmed via a `printToLog` tree dump before landing on the real cause.
- New/changed test classes run alone first (`RewardEditScreenUiTest`, `TaskEditScreenUiTest`, `SettingsScreenUiTest`, `CleanUpScreenUiTest`, `TaskLibraryScreenUiTest`, `SharedDialogsCancelUiTest`), then the full instrumented suite (102/103 pass on the first full run; the one failure — a pre-existing, untouched test — and a second pre-existing test that failed on a separate full run were each re-run alone and passed cleanly, confirming emulator flakiness under long continuous runs rather than a regression).
- No `AppModule`/`TestAppModule`/`@Inject` changes; `assembleDebugAndroidTest` was still run repeatedly during development to catch compile errors early.
- `TESTING.md`: new rows for `CleanUpScreenUiTest`, `TaskLibraryScreenUiTest`, `SharedDialogsCancelUiTest`; existing `RewardEditScreenUiTest`/`TaskEditScreenUiTest`/`SettingsScreenUiTest` rows updated with their new cancel-path cases; a new "Covered" entry describing the shared helper and the `isDialog()`/`Espresso.pressBack()` techniques; instrumented total (~90 → ~105) and UI-tier pyramid count (~55 → ~70) updated to match.
- `CLEANUP_BACKLOG.md`: this item removed now that it's actioned.

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
