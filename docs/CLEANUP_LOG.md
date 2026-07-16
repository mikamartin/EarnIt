# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 44 — `fix/reward-edit-task-state-loss` branch

Fixes [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s Correctness item: adding two new tasks in a row via "Create your own" to an unsaved reward silently dropped the first. Root cause: `RewardEditScreen.kt`'s `taskState`/`taskStateReady` were plain `remember`, reset every time Navigation Compose disposed the screen's composition for a `TaskEditScreen` round-trip; the startup effect then re-derived inclusion from the still-unsaved (task-less) reward. Fix: a custom `Saver` so `taskState` survives via `rememberSaveable`, the same mechanism already used by the screen's other form fields.

#### Duplication ✅ (checked, none found)
- Grepped for existing `Saver(`/`listSaver`/`mapSaver` usage across `app/src/main/java` — none exists yet, so `TaskStateMapSaver` is the first custom `Saver` in the codebase rather than diverging from or duplicating an established one.

#### Decoupling ✅ (n/a)
- Pure UI-state persistence change inside a single composable; no business logic touched.

#### Complexity & Pattern Health ✅ (reviewed remember vs rememberSaveable across the file)
- Reviewed every other `remember`/`rememberSaveable` in `RewardEditScreen.kt` while fixing this one: `showAddTaskDialog`/`showIconPicker`/`showDeleteDialog` are transient dialog-visibility flags, correctly reset on recomposition; `pendingRewardSaveNav` is a short-lived flag that never spans a `TaskEdit` round-trip, so plain `remember` is correct there too. Only `taskState`/`taskStateReady` needed the fix.
- `TaskStateMapSaver`'s `step 4` restore loop pairs directly with the 4-element tuple built two lines above in `save` — left inline rather than a named constant since the two are adjacent in one small function and self-evidently paired.

#### Dead Code & Hygiene ✅
- ktlint clean.
- `git status` clean apart from the intended files: `RewardEditScreen.kt`, `RewardEditScreenUiTest.kt`, `CLEANUP_BACKLOG.md`, `DEV_PLAYBOOK.md`, `TESTING.md`.

#### Naming Consistency ✅
- `TaskStateMapSaver` follows Kotlin's PascalCase convention for a `val` holding a reusable, type-like instance; no existing `Saver` naming precedent to match since this is the first one in the codebase.

#### Hardcoded Values ✅ (n/a)
- None introduced.

#### Accessibility ✅ (n/a)
- No UI elements touched.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (checked, no changes needed)
- Grepped `EARNIT_SPEC.md` for `taskState`/related terms — this is an internal persistence detail, not spec-level behavior, so no drift to reconcile. Checked the Deferred Ideas section for anything related — none.

#### Tests ✅ (1 test flipped, no new gap)
- `RewardEditScreenUiTest.sequentialCreateNewTasks_onUnsavedReward_onlyLastOneStaysIncluded`, which pinned the bug as a red-test-in-waiting (added in Pass 41), was flipped to `...bothStayIncluded` and now asserts both tasks survive the round-trips — this is the regression test the fix needed, already in place rather than a new gap to close.
- `RewardEditScreenUiTest` re-run alone on a connected emulator: 10/10 pass. Full instrumented suite re-run: 84/84 pass, no regressions.
- `./gradlew ktlintCheck`, `test`, and `assembleDebug` all pass, run sequentially per `CLAUDE.md`. No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required on its own — covered anyway by the full `connectedDebugAndroidTest` run.
- `TESTING.md` updated: the `RewardEditScreenUiTest` row's description of the sequential-create scenario now describes the fixed behavior instead of the pinned bug; the "True process death and restore" Not Covered entry updated to reflect that `RewardEditScreen`'s `taskState`/`taskStateReady` now survive via `rememberSaveable` (only `TaskEditScreen`'s `rewardLinkState` remains on plain `remember`).
- `DEV_PLAYBOOK.md` Known Limitations: the `taskState` half of the rotation-loss bullet removed now that it's fixed; `rewardLinkState` (TaskEditScreen) stays, since fixing it was explicitly kept out of scope for this branch (confirmed with the user).
- `CLEANUP_BACKLOG.md`: the Correctness section (this item) removed now that it's actioned.

---

### Pass 45 — `test/process-death-restore` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s "True process death and restore" item: no automated test proved that Room-backed data survives a cold start, as opposed to `rememberSaveable` Bundle state (only covered via `activityRule.scenario.recreate()`, which preserves the Bundle). Research during planning found the item's original premise — a real `am force-stop` via a new `uiautomator` dependency — unusable here: this repo runs instrumented tests with no Test Orchestrator configured, so the test and the app under test share one OS process, and force-stopping the package would kill the test itself mid-method. Confirmed with the user: build an approximation instead (close the managed `ActivityScenario`, launch a brand-new one with no Bundle) rather than new script/CI tooling to drive a real two-phase kill.

#### Duplication ✅ (checked, none found)
- New test's setup (create task, create reward, link, log) mirrors `HappyPathTest`'s repository-level seeding verbatim rather than reinventing it.

#### Decoupling ✅ (n/a)
- Test-only file; no production code touched.

#### Complexity & Pattern Health ✅ (checked)
- `ProcessDeathRestoreTest.kt` is ~55 lines, one test method, no nested lambdas beyond the existing `runBlocking` seeding pattern.

#### Dead Code & Hygiene ✅ (checked)
- ktlint clean.
- `git status` clean apart from the intended files: `ProcessDeathRestoreTest.kt`, `TESTING.md`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅
- `ProcessDeathRestoreTest` follows the existing `*Test.kt` convention and sits flat in `app/src/androidTest/java/com/earnit/app`, matching every other UI test file's placement.

#### Hardcoded Values ✅ (n/a)
- Test literals ("Morning Run", "Coffee Treat") match `HappyPathTest`'s existing fixture values rather than introducing new ones.

#### Accessibility ✅ (n/a)
- No UI code touched.

#### Deprecated APIs ✅
- None used.

#### Spec Review ✅ (checked, no changes needed)
- Grepped `EARNIT_SPEC.md` for process-death/cold-start terms — the one hit (widget flash timer surviving process death) is unrelated to this test. No drift to reconcile.

#### Tests ✅ (1 new file, docs updated)
- New instrumented test run alone on a connected emulator first to confirm the close-and-relaunch technique actually works before trusting it: pass. Full instrumented suite re-run: 88/88 pass, no regressions.
- No `AppModule`/`TestAppModule`/`@Inject` changes, but `./gradlew assembleDebugAndroidTest` was run anyway given the new file — confirms `ActivityScenario` resolves without adding the `uiautomator` dependency the backlog item originally assumed was needed (unused once the approach changed from a real `am force-stop` to the in-process approximation).
- `./gradlew ktlintCheck`, `test`, `assembleDebug`, and `connectedDebugAndroidTest` all pass, run sequentially per `CLAUDE.md`.
- `TESTING.md`: new `ProcessDeathRestoreTest` row added to the Instrumented Tests table; the "True process death and restore" entry moved from "Not Covered" to a new "Covered" entry describing exactly what the approximation does and doesn't prove; instrumented total (~85 → ~90) and UI-tier pyramid count (~50 → ~55) nudged to match the actual current count (88), which had drifted past its prior rounding bucket.
- `CLEANUP_BACKLOG.md`: this item removed now that it's actioned.

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
