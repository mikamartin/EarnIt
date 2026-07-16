# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 43 — `fix/reward-progress-bar-contrast` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s Visual Polish item, flagged during Pass 42: `RewardDetailScreen.kt`'s progress bar showed the current-points number in dark, muted text below ~12% progress instead of the white it used above that threshold, making low-progress rewards look inconsistent with further-along ones.

#### Duplication ✅ (checked, none found)
- Grepped for `Shadow(` across `app/src/main/java` — this is the only text-shadow use in the codebase, so the fix introduces a new pattern rather than duplicating or diverging from an existing one.

#### Decoupling ✅ (n/a)
- Pure rendering change inside a single composable; no business logic involved.

#### Complexity & Pattern Health ✅ (net reduction, verified across themes)
- The two conditional branches (`progress <= 0.12f` dark-text vs. white) collapsed into one: the number is now always white, right-anchored via `weight(progress.coerceAtLeast(0.12f))` instead of switching layouts at the threshold. Read all of `ColorSchemes.kt` to confirm the fix holds for every `AppColorScheme` (Warm Gold, Ocean Blue, Forest) and both light/dark mode — the unfilled track background is a fixed cream gradient independent of theme, so a text shadow (not a theme-specific tweak) is sufficient for legibility in every combination.

#### Dead Code & Hygiene ✅ (checked)
- `ktlintCheck` passed (catches unused imports; `Shadow` import is used).
- `git status` re-checked after the doc edits below — clean apart from the intended files: `RewardDetailScreen.kt`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅ (n/a)
- No new symbols beyond a single local `val textAnchor`.

#### Hardcoded Values ✅ (reviewed, left inline — not a gap)
- The shadow's alpha (`0.8f`), offset, and blur radius are new magic numbers, but single-use and commented in place. Not promoted to a named constant: nothing else in the file needs them, so a constant would add an abstraction without a second caller to justify it.

#### Accessibility ✅ (n/a)
- No new interactive elements.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (checked, no changes needed)
- Grepped `EARNIT_SPEC.md` for progress-bar-related terms — it doesn't describe this rendering-level detail (only that a progress bar exists), so no drift to reconcile.

#### Tests ✅ (checked, no gap — not just skipped)
- Grepped `app/src/androidTest` for the old threshold/color/field name — the only `totalPoints` hits are repository-level point-balance assertions in `StartOverTest`/`HappyPathTest`, unrelated to this rendering branch. No existing test needed updating.
- No automated test added for the color/shadow itself: not `Repository`/`ViewModel` logic, and the codebase has no screenshot/pixel-level testing infrastructure anywhere (checked against the pattern in every prior pass) — Compose's testing APIs don't cleanly assert rendered text color without pixel diffing. Manual on-device verification is the established way visual-only changes get checked here (matches Pass 42's banner-removal precedent). Confirmed manually across progress levels, with shadow opacity tuned from the user's on-device feedback (0.45 → 0.7 → 0.8).
- `./gradlew ktlintCheck`, `assembleDebug`, and `test` all pass, run sequentially per `CLAUDE.md`. No instrumented-suite run — no androidTest files changed.

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
