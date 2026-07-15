# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 42 — `refactor/split-home-screen` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s `HomeScreen.kt` item (~320 lines in one composable) — same pattern as the `RewardDetailScreen`/`TaskEditScreen`/`SettingsScreen`/`RewardEditScreen` splits: pull screen-only pieces into `private` composables in the same file, no new files/folders. One deliberate behavior change was made alongside the split (confirmed with the user, not silently bundled): the bottom slide-up "max reward banner" (`showMaxBanner`, its `LaunchedEffect`, and its UI) was removed entirely, since it duplicated the FAB tooltip's message in a second on-screen location. The FAB tooltip itself was kept, with `Strings.MAX_REWARD_BANNER` renamed to `MAX_REWARD_TOOLTIP` to match what it now actually is.

#### Duplication ✅ (reviewed, nothing new)
- No styling/gradient patterns repeated inline beyond what already existed pre-split; the extracted helpers carry code forward verbatim.

#### Decoupling ✅ (reviewed, nothing to change)
- `HomeDialogs` takes the full `EarnItViewModel` — checked whether this could be narrowed to specific lambdas, but `LogTaskDialog`/`ClaimDialog` (`SharedDialogs.kt`) already require the full `viewModel` in their own public signature and are shared with `RewardDetailScreen`, so narrowing would mean changing a shared dialog's API, out of scope for a structural-split branch.
- `homeRewardListItems` (a `LazyListScope` extension, same shape as Pass 41's `rewardEditTasksSection`) takes `isDragging`/`draggingIndex`/`draggingRewardId` as individual value+callback pairs rather than bundled state. Confirmed via grep that `draggingIndex`/`draggingRewardId` are read/written only inside this function, while `isDragging` is also read by the orchestrator's list-sync `LaunchedEffect` — so at least `isDragging` must stay hoisted. `LazyListScope`'s content lambda isn't itself a composable context, so `remember` can't live inside `homeRewardListItems` directly; hoisting is required, not avoidable. Bundling the drag-index/id pair into one hoisted value would trim the 15-parameter signature but changes state shape, not just relocates it — left as a future opportunity if this file is touched again, not actioned here.

#### Complexity & Pattern Health ✅
- `HomeScreen(...)` cut from ~320 lines to a ~131-line orchestrator (state + 3 `LaunchedEffect`s + dialogs call + `Column { HomeHeader, LazyColumn, HomeAddRewardFab }`) plus 4 private helpers: `HomeHeader`, `HomeDialogs`, `BoxScope.HomeAddRewardFab`, `LazyListScope.homeRewardListItems`.
- Confirmed via grep that each of the 4 new helpers has exactly one call site — reviewed against "is this extraction earning its keep": yes, consistent with the single-caller pattern already accepted in Pass 39/41.

#### Dead Code & Hygiene ✅ (1 fix)
- **Fixed:** a stale comment in `RewardLimitUiTest.kt` still said "MAX_REWARD_BANNER text becomes visible" after the `Strings` constant was renamed to `MAX_REWARD_TOOLTIP`.
- No unused imports (ktlint clean on every check, re-verified after all edits).
- Confirmed via diff that no new `Color(0xFF...)` literals were introduced.
- `git status` clean apart from the intended files: `HomeScreen.kt`, `RewardLimitUiTest.kt`, `SettingsScreenUiTest.kt`, `Strings.kt`, `EARNIT_SPEC.md`, `CLEANUP_BACKLOG.md`, `CLEANUP_LOG.md`.

#### Naming Consistency ✅
- New composables follow the existing `Home*`/`*Dialogs` naming already established (`HomeDialogs` ↔ `RewardEditDialogs`/`TaskEditDialogs`).

#### Hardcoded Values ✅
- None introduced; existing values (16.dp, 56.dp, etc.) carried over unchanged.

#### Accessibility ✅ (reviewed, nothing new)
- Confirmed via `git show HEAD` that the mascot `Image`'s `contentDescription = null` predates this branch — not introduced or touched by the split.
- The FAB's `Icon` keeps its existing `contentDescription = Strings.NEW_REWARD_DESC`, unchanged.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (1 fix)
- **Fixed:** `EARNIT_SPEC.md` §6 Settings table's Max Reward Count row still described "A separate 3s banner also auto-appears the moment a new reward reaches the cap" (added in Pass 39) — that banner was removed as part of this split, so the row was trimmed to describe only the tooltip behavior that remains.
- Checked §10 Screen Map's "Prizes (Home)" row against current behavior (progress cards, mascot, quote of the day) — still accurate, no further drift.

#### Tests ✅ (1 fix, 1 new gap logged, 1 unrelated gap found and logged)
- Coverage reviewed for the area before finalizing: FAB→RewardEdit navigation under the cap (`SaveNavigationUiTest`), FAB tooltip at the cap (`RewardLimitUiTest`, `SettingsScreenUiTest`), empty state (`EmptyStateUiTest`), show-quote toggle (`SettingsScreenUiTest`), card tap/log/claim dialogs (`UiHappyPathTest`), and reorder persistence (`SortOrderTest`, unit) are all still exercised and unaffected by the split.
- **Gap found and logged, not fixed here:** the drag-to-reorder gesture itself (relocated into `homeRewardListItems`) has no automated coverage at any level and isn't mentioned in `TESTING.md`'s Tier 4 or "Not Covered" sections — pre-existing, not introduced by this split. Logged as a new `CLEANUP_BACKLOG.md` Test Coverage item rather than left unmentioned.
- No test ever exercised the removed bottom banner's own behavior (its 3s auto-show/hide timing), so nothing needed to be deleted for its removal.
- `TESTING.md` required no count updates — no tests added/removed by this pass, and its existing `RewardLimitUiTest`/`SettingsScreenUiTest` rows already described only the tooltip.
- **Found, unrelated to this branch's scope:** `RewardDetailScreen.kt` (not touched by this split) renders its current-points number in dark, muted `onSurfaceVariant` text below ~12% progress instead of the white it uses once the fill takes over, making low-progress rewards look inconsistent with further-along ones — noticed during the user's manual pass on unrelated test data. Logged as a new `CLEANUP_BACKLOG.md` Visual Polish item since it's pre-existing and outside this branch.
- `./gradlew ktlintCheck`, `assembleDebug`, `test`, and `connectedDebugAndroidTest` (full suite, 84/84) all pass, run sequentially per `CLAUDE.md`.

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
