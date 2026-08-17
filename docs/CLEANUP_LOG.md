# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 64 — `test/close-manual-test-plan-gaps` branch

Reviewed `MANUAL_TEST_PLAN.md` against the actual code to check whether any journey marked manual-only was really reachable by an instrumented test. Two were: the widget's ADD TASK button (`autoOpenAddTask`/`rewardId` intent extras into `MainActivity`) and `WidgetConfigActivity`'s label field's character cap — both are plain `ComponentActivity`/nav-graph code reachable directly, without a real widget host. Added coverage for both; also trimmed a redundant manual mascot-suppression check from the Export/Import journey (already unit-tested).

#### Duplication ✅ — found and fixed
First pass added the deep-link test as its own file (`WidgetAddTaskDeepLinkUiTest`, 1 test). Running this checklist's file-threshold item against it surfaced that `SaveNavigationUiTest.homeCardAddTasksButton_opensAddTaskDialogDirectly` already covers the identical downstream nav effect (`autoOpenAddTask`) via a different trigger (in-app click vs. intent extras) — same premise as that file's own stated scope ("shortcut navigation... opens the Add Task dialog directly"), and the new file sat below the "3+ tests for a cohesive new behaviour" guideline. Merged it into `SaveNavigationUiTest` as a 6th test and deleted the standalone file.
Considered further: the "close the rule's default activity scenario, launch a fresh one with a custom `Intent`" boilerplate (4-5 lines) now appears in 3 places — `ProcessDeathRestoreTest`'s cold-start relaunch, the merged test above, and `MaxLengthUiTest`'s new widget-label test — each launching a different activity/intent shape (same-class relaunch vs. two different activity classes with different extras). Left inline rather than extracting a shared helper; a parameterized version would add more indirection than the handful of lines it'd save across 3 meaningfully different call sites.

#### Decoupling / Complexity & Pattern Health / Hardcoded Values / Accessibility / Deprecated APIs ✅ (n/a)
No production code touched — test files and docs only.

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` passes. `git status` confirms exactly the intended files changed, including the deletion of the superseded standalone test file.

#### Naming Consistency ✅ (checked)
New test method names (`widgetAddTaskIntent_navigatesToRewardDetailWithDialogAlreadyOpen`, `widgetLabelInput_isCappedAtMaxChars`) follow the existing `subject_expectedOutcome` convention already used by every sibling test in both files.

#### Spec Review ✅ (checked, no update needed)
Both new tests cover pre-existing, already-documented behavior — `EARNIT_SPEC.md` already describes `autoOpenAddTask` and the widget label field — no new behavior was introduced.

#### Tests ✅ — found and fixed (see Duplication above for the file-threshold finding)
- Both new tests run on a connected emulator (API 36, two devices) — 0 failed, 0 skipped, both before and after the merge.
- `checkInstrumentedTestTags` passes: `SaveNavigationUiTest` and `MaxLengthUiTest` both already carried their required `@UiTest` layer tag; added `@Widget` as an additional optional tag to both since they now cover widget-triggered behavior.
- No `AppModule`/`TestAppModule` changes — both classes' new `@Inject EarnItRepository` sites reuse an existing binding other test classes already use. Ran `./gradlew assembleDebugAndroidTest` anyway (before and after the merge) to confirm compile.
- `TESTING.md`: updated per-file counts (`MaxLengthUiTest` 5→6, `SaveNavigationUiTest` 5→6) and descriptions; header/cadence-table aggregate 110→112 (exact, matches actual `@Test` count). `EARNIT_SPEC.md`'s test summary line had pre-existing drift unrelated to this pass (182/25/108/30/74 vs. actual 189/26/112/32/78) — corrected all five figures while already touching that line.
- `MANUAL_TEST_PLAN.md`: removed the Export/Import mascot-suppression checks (old steps 7 and part of 8) — that behavior is already unit-tested (`MascotNotificationTest`'s "seeds unlocked mascots silently without triggering notification"; confirmed it covers both replace and merge, since `checkAndUnlockMascots(silent = true)` in `importFromFile` isn't gated on the `replace` flag) and isn't a system-boundary concern within the manual plan's own stated scope.
- `./gradlew ktlintCheck`, `test`, `assembleDebug`, `assembleDebugAndroidTest` all pass sequentially per `CLAUDE.md`.

---

### Pass 65 — `fix/claimed-rewards-earn-again-icon` branch

UX: the Claimed Rewards tab's "Earn Again" button was a solid pill (`secondary` background, bold white text) with the same visual weight as the reward name itself — competing with the "you earned this" moment the tab is for. Replaced it with an icon-only button: `Icons.Default.Refresh` (the app's established "repeat this" glyph, already used the same way on `RewardEditScreen`/`RewardDetailScreen`/`TaskEditScreen`/`SharedDialogs`) inside a small `secondaryContainer`-tinted circle, wrapped in `IconButton` so the actual tap target stays 48dp even though the visible circle is 32dp. Confirmed the direction and the specific subdued-vs-tonal-circle styling with the user before implementing (`AskUserQuestion`, two mocked options) per `CLAUDE.md`'s "confirm before non-trivial changes" rule.

While answering "does this have test coverage" as a follow-up question, found the button itself had none at either layer: `RepositoryBehaviourTest` covers `copyRewardFromEntry` (the data operation) but nothing exercised the on-screen button, and `EmptyStateUiTest` visits the Claimed Rewards tab but only for its empty state. Added a first instrumented test for it — which is what put the button in front of the user for manual testing, surfacing four real bugs in `copyRewardFromEntry` that predate this branch entirely (the icon-vs-pill change never touched this logic) but had never been exercised on a real device before:

1. **No feedback on tap.** Nothing told the user the reward had been re-added.
2. **Unbounded duplicates.** Tapping repeatedly created multiple active rewards sharing the same name — the app enforces "no duplicate active reward names" everywhere else (`RewardEditScreen`'s Save-button guard), but `copyRewardFromEntry` had no such check.
3. **Description silently dropped.** `HistoryEntryEntity` only snapshots name/icon/cost at claim time, never description, so every copy came back with `description = ""` regardless of what the original had. Tasks and mandatory/repeatable flags *did* carry over correctly (verified with both a raw-repository test and a full Compose-UI test before touching any code, to isolate what was actually broken) — the user's own follow-up report ("i would assume tasks would also be added... but that doesn't happen either") turned out to describe the description gap, not tasks.
4. **Max Reward Count ignored.** `HomeScreen`'s FAB blocks reward creation once the active list hits the configured cap (`settings.maxRewardCount`), but that gate lives only in the FAB's own click handler — `copyRewardFromEntry` is a second reward-creation path that never went near it, so re-adding from History could push the active list past the limit entirely unchecked.

Fixed all four: `HistoryScreen.kt` now shows a snackbar after every tap (success, "already exists", or the same max-limit message the FAB uses); `copyRewardFromEntry` checks both the name conflict and the active-reward-count cap *inside its own `database.withTransaction` block* (not a possibly-stale `uiState`/`settings` snapshot) before inserting, mirroring the exact guard-inside-transaction pattern `logCompletion`'s non-repeatable-task check already uses against the identical class of double-tap race (`ConcurrentLogCompletionTest` proves that pattern closes the race for logging; this reuses it for both new guards); and it now reads the still-existing archived original reward's row directly (`rewardDao.getReward(entry.rewardId)`) for its `description`, since archiving never deletes the row — no schema change needed.

A follow-up question ("does our test data actually show tasks for claimed rewards?") surfaced a fifth, dev-tooling-only gap: `TestDataSeeder.seedFull`'s `history()` helper inserted `HistoryEntryEntity` rows with a placeholder `rewardId = 0L` and never created a backing `RewardEntity`/`RewardTaskCrossRef` at all — legal only because `HistoryEntryEntity.rewardId` deliberately has no FK constraint (so history survives its source reward being deleted later). Every one of the 20 seeded "claimed rewards" would silently no-task/no-description on Earn Again regardless of the fixes above, since there was nothing real for `copyRewardFromEntry` to look up. Rewrote `history()` to insert a real archived `RewardEntity` plus cross-refs for the tasks actually logged against it (first task mandatory, rest repeatable-optional) before the history entry and logs — mirroring what `EarnItRepository.claimReward` actually produces, rather than a shortcut. Left four names deliberately colliding with an Active Reward elsewhere in the seed set (Spa Day, Gaming Session, New Book, Nice Dinner) so the seeded dataset also exercises the duplicate-name guard, not just the happy path.

#### Duplication ✅ (checked)
The duplicate-name check reuses `Strings.rewardDuplicateError(name)` — the exact string `RewardEditScreen` already shows for the identical invariant — rather than adding a second string for the same concept. Button styling unchanged from the icon-swap: still `MaterialTheme.colorScheme.secondaryContainer`/`onSecondaryContainer`, consistent across all three color themes.

#### Decoupling ✅ — found and fixed
Both new guards were originally going to live in the Composable (mirroring `RewardEditScreen`'s existing pattern, which checks `uiState.rewardProgressList` directly in the UI layer, and `HomeScreen`'s FAB, which checks `settings.maxRewardCount` the same way). Rejected that in favor of putting both inside the repository's own transaction instead — not just for architectural cleanliness, but because a UI-layer check against a `StateFlow` snapshot can't close the actual double-tap race (two rapid taps can both read "no conflict"/"under the cap" before either write commits); only a check inside the same transaction as the write can. `copyRewardFromEntry` now takes `maxActiveRewards` as a parameter (sourced from `settings.value.maxRewardCount` in the ViewModel, since `EarnItRepository` doesn't depend on `SettingsRepository` and shouldn't gain that coupling for one call site) and returns a `CopyRewardOutcome` enum (`ADDED`/`NAME_CONFLICT`/`MAX_REWARDS_REACHED`) instead of a bare `Boolean`. `EarnItViewModel.copyRewardFromEntry` forwards it through an `onComplete: (CopyRewardOutcome?) -> Unit` callback — the same style already used by `deleteReward`/`exportToFile` — so the Composable only decides which snackbar text to show, not whether the copy is allowed.

#### Complexity & Pattern Health ✅ (checked)
`IconButton` reused as before. The new `CopyRewardOutcome` enum follows the same small-enum-for-a-decision-function shape already used by `WidgetActionButton`/`AppColorScheme`/`MascotId`, rather than a heavier sealed class. The guard logic added to `copyRewardFromEntry` is a few linear lines inside the existing `withTransaction` block, not a new abstraction.

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck`/`ktlintFormat` pass. `git status` confirms exactly the intended files changed.

#### Naming Consistency ✅ (checked)
`EarnAgainButtonUiTest` follows the existing `<Subject>UiTest` file-naming convention. New test method names follow the file's own `subject_expectedOutcome` style.

#### Hardcoded Values ✅ (checked)
No new hex colors or magic numbers — reused `Strings.rewardDuplicateError`/added one small `Strings.historyEarnAgainAdded(name)` function, matching the existing name-interpolated-message convention (`mascotUnlocked`, `taskDuplicateError`).

#### Accessibility ✅ (checked, unchanged from icon-swap)
`contentDescription`/48dp tap target from the earlier part of this pass unaffected by the bug fixes.

#### Deprecated APIs ✅ (n/a)

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md`'s History section already documented this feature under the name "Reactivate" ("allows copying an archived reward back to the active reward list, preserving its task links") — accurate but incomplete once description-preservation, duplicate-prevention, and the reward-count cap were added. Updated the line to cover all three and noted the UI calls it "Earn Again" (a pre-existing naming mismatch between spec and UI text, left as-is — out of scope for this pass, not a behavior bug).

#### Tests ✅ — found and closed four real gaps, not just a coverage gap
- `RepositoryBehaviourTest.kt`: extended `copyRewardFromEntry copies icon and appends reward to end of list` to also assert description is preserved (renamed to reflect that); added `copyRewardFromEntry skips creating a duplicate when an active reward already has this name`, `copyRewardFromEntry allows the copy when only an archived reward has this name` (the critical boundary case — the just-claimed original itself shares the name and is archived, and must *not* self-block re-adding it, or the feature would be unable to ever run twice), and `copyRewardFromEntry blocks the copy when the active reward count is already at the max`.
- `EarnAgainButtonUiTest.kt`: grew from 1 test to 4 — the original name/cost check, `tapEarnAgain_carriesOverLinkedTaskAndDescription` (seeds a linked task and a description, asserts both render on the new reward's detail screen), `tapEarnAgain_whenActiveCopyAlreadyExists_doesNotCreateASecondOne` (taps twice, asserts the second tap shows the duplicate-error snackbar and only one active copy exists in the DB), and `tapEarnAgain_whenAtMaxRewardCount_doesNotExceedTheLimit` (seeds an unrelated reward to fill a cap of 1, asserts the FAB's own `MAX_REWARD_TOOLTIP` text appears and the active list still has exactly the one pre-existing reward).
- Before writing any fix, wrote a throwaway repro test (`ScratchReproTest`/`ScratchReproUiTest`, deleted before committing) to check the user's "tasks not carried over" report directly against a real Room database and against the full Compose UI stack — both showed tasks carrying over correctly, which is what redirected the investigation to the actual bug (description) instead of chasing a task-copy bug that didn't exist.
- Ran the full `@Reward`-tagged instrumented suite (59 tests, not just the new file) on a connected emulator after each repository change, since `copyRewardFromEntry`'s signature changed twice (`Unit` → `Boolean` → `CopyRewardOutcome?`) and it's exercised by several other reward-flow tests — 0 failed both times.
- One test-only flake found and fixed along the way: the duplicate-tap test's second `waitUntil` was set to 5s, but Material3 only shows one snackbar at a time — the first tap's `SnackbarDuration.Short` (~4s) was still queued/visible, so the second message's wait needed more headroom. Bumped to 10s.
- Verified the `TestDataSeeder` rewrite directly on a connected emulator with another throwaway repro test (`ScratchSeedCheckTest`, deleted before committing): ran `seedFull`, confirmed all 20 history entries now carry a real `rewardId`, that Earn Again on the seeded "Spa Day" entry returns `NAME_CONFLICT` against the active "Spa Day" reward as intended, and that Earn Again on "Date Night" returns `ADDED` with the expected mandatory/optional task split (`Workout` mandatory; `Morning Run`/`Code Practice`/`Yoga Session`/`Meditate` optional-repeatable) actually present on the new reward. `TestDataSeeder` itself has no automated test coverage (it's a dev-only manual tool, gated behind `devModeEnabled`, triggered from `DataScreen`'s dev section) and isn't tracked in `TESTING.md`'s suite — this was a manual verification pass, not a permanent test addition.
- `TESTING.md`: `RepositoryBehaviourTest` 12→15, `EarnAgainButtonUiTest` 1→4 (both exact, per-file counts); rewrote the "Earn Again" edge-case paragraph to describe all four bugs, the `CopyRewardOutcome` return type, and the transaction-based guards instead of just the coverage gap; aggregate counts — Unit 189→192 (exact), instrumented header/cadence-table 115→116 (exact, matches actual `@Test` count), UI pyramid stays at 80 (actual 82 still rounds to 80).
- `EARNIT_SPEC.md`'s test-summary line updated to match: 192 unit / 116 instrumented / 82 UI.
- `MANUAL_TEST_PLAN.md`: checked, no update needed — this flow doesn't cross a system-process boundary.
- `CLEANUP_RULES.md`: added a new "Dev Seed Data" section — nothing in the checklist previously asked whether dev/manual seed data actually covers the conditions it's meant to, or whether it mirrors real production data shape rather than a shortcut like the placeholder-`rewardId` gap found here. Future passes now check this explicitly instead of it only surfacing when a user happens to manually test against the seeded dataset.
- Re-ran this checklist a second time against the accumulated diff once the branch stopped growing, rather than trusting the log entries written progressively while each bug was still being found. One genuine gap surfaced: `EarnAgainButtonUiTest`'s class doc comment still only described the name-conflict guard, predating the `MAX_REWARDS_REACHED` guard added afterward — updated it to describe all three outcomes. Everything else (duplication, decoupling, naming, hardcoded values, accessibility, dead code, every `copyRewardFromEntry` call site on its current signature) re-checked clean; `checkInstrumentedTestTags` and the full build/test/instrumented sequence re-run and pass after the fix.
- `./gradlew ktlintCheck`, `test`, `assembleDebug`, `assembleDebugAndroidTest` all pass sequentially per `CLAUDE.md`.

---

### Pass 66 — `feature/onboarding-tutorial` branch

Ran against the whole branch: the two original feature commits (`4fc7244` feat, `0dbe1f9` test) plus this session's UX/copy pass and bug fixes on top, reviewed together as one diff against `main`.

#### Duplication ✅ (checked)
`RewardIconAndNameField`/`TaskIconAndNameField` are near-identical (icon button + name field + conditional duplicate-name error) — but that predates this branch (present on `main` before onboarding work started). This session touched both identically to fix the layout bug below, without adding new duplication. Considered extracting a shared composable; didn't — the two differ in max-char constant, default icon glyph, label, and error-string function, enough that a shared version would need nearly as many parameters as either version's own body saves. No inline strings bypassing `Strings.kt`, no hardcoded colors.

#### Decoupling ✅ (checked, n/a)
No new coupling introduced. `OnboardingStep` living in the same file as Compose UI code, and the ViewModel using `mutableStateOf` directly for `onboardingStep`/`sessionNickname`, are both pre-existing patterns from the original feature commits — unchanged this session.

#### Complexity & Pattern Health ✅ — found and fixed two real bugs
1. **Layout-overlap bug.** `TaskPointsSection`, `TaskIconAndNameField`, and `RewardIconAndNameField` each emit more than one top-level composable (a toggle row + a conditional sliders card + a totals row; or a row + a conditional error text). Wrapping a call like that in a bare `Box` (for onboarding anchor-capture) breaks Column-based vertical stacking — `Box` overlays multiple children instead of stacking them, which is exactly why "Total points" rendered on top of "Time" once auto-points was on, and would have identically hidden the duplicate-name error text behind the name field. Fixed by giving each composable its own `modifier` parameter and a single root `Column`, called with `modifier = Modifier.captureOnboardingAnchor { }` directly instead of an external `Box` wrapper.
2. `taskCoachStep` used raw `Int` literals (`0`/`1`) for a two-state sub-flow, inconsistent with the rest of this same feature's typed state (`OnboardingStep`/`OnboardingField` sealed types/enums). Renamed to a `Boolean` (`taskCoachedOnPoints`).

Also noted, not fixed: `TaskEditScreen` (196 lines already on `main`, before this branch) and `RewardEditScreen` (252 lines already after the original onboarding commits, before this session) both exceed the ~150-line composable guideline. This session added ~49 lines to `TaskEditScreen` for the coaching bubble and a net +1 line to `RewardEditScreen`. Considered extracting the coaching scrim+bubble into a helper composable; not done — the scrim needs to render inside the scrollable `Box` (to dim just that section) while the bubble renders outside it in the same `Column`, so there's no single natural call site to extract to without restructuring the box hierarchy.

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` passes. `git status` shows exactly the 8 intended files changed, no stray untracked files. No TODO/FIXME introduced, no commented-out code.

#### Naming Consistency ✅ (checked)
New `Strings.kt` constants (`ONBOARDING_TASK_LINKED_LINE`, `ONBOARDING_TASK_NAME_LINE`, `ONBOARDING_TASK_POINTS_LINE`, `ONBOARDING_SAVE_BLOCKED_LINE`, `ONBOARDING_STARTER_CHIPS_LABEL`) follow the existing `ONBOARDING_*` convention. No new files this session.

#### Hardcoded Values ✅ (checked)
No new hex colors. The `taskCoachStep` magic-number fix is covered under Complexity above.

#### Accessibility / Deprecated APIs ✅ (n/a)
No new icon-only buttons or tappable targets, no deprecation warnings from this session's changes.

#### Spec Review ✅ — found and fixed a significant gap
`EARNIT_SPEC.md` didn't mention the onboarding tutorial at all — a full first-launch feature, entirely undocumented. Added §3a ("First-Launch Onboarding Tutorial") describing the flow, the task-creation detour, and the save-blocked state; added an "Onboarding Seen" row to the App Settings table cross-referencing it.

#### Tests ✅ — found and closed real coverage gaps
- The entire two-step task-creation coaching sub-flow (name → points), the "task linked" copy switch, and the "Save blocked" fallback had zero coverage despite being added mid-session. Extended `OnboardingFlowUiTest`'s happy-path test to assert all three; added `replayTutorial_duplicateRewardName_blocksSaveWithExplanation`, which seeds a reward whose name matches a starter chip, replays the tutorial, picks that same chip, and confirms Save is disabled with the explanatory message shown instead of a silently-dead button.
- Ran the extended and new tests on a connected emulator. A second physical device joined the adb pool mid-session, causing an unrelated `IllegalStateException: No compose hierarchies found in the app` on `connectedDebugAndroidTest` (it runs across every attached device by default); confirmed as environment noise rather than a regression by re-running pinned to the emulator alone (`ANDROID_SERIAL=emulator-5554`) — 0 failed, both before and after the `taskCoachedOnPoints` rename.
- `TESTING.md`: added `OnboardingStepTest` (11) and `OnboardingFlowUiTest` (3, up from 2) rows. While updating, found the doc's file counts (26 unit / 33 instrumented files) were already stale on `main` itself, unrelated to this branch — actual was 28 / 40 — corrected to current true figures (29 / 41) instead of adding this branch's delta on top of a wrong baseline. Aggregate test counts rounded per the doc's own stated convention (ballpark, not a maintained tally): Unit 189/192 (the pyramid and section header disagreed with each other before this pass) → 205; Instrumented 116 → 120; UI 80/82 → 85.
- `EARNIT_SPEC.md`'s own test-summary line updated to match, kept exact per that line's existing convention: 203 unit / 119 instrumented / 85 UI.
- `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest` all pass. `connectedDebugAndroidTest` against `OnboardingFlowUiTest` (pinned to the emulator) — 0 failed.

#### Dev Seed Data ✅ (checked, n/a)
Onboarding is specifically a first-launch/clean-state flow — it doesn't read or depend on `TestDataSeeder`'s dev-mode seed data.
