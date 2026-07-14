# EarnIt — Cleanup Backlog

A punch list from a whole-repo audit against [DEV_PLAYBOOK.md §1 Post-Work Cleanup](DEV_PLAYBOOK.md), run against `main` at `59d9a82` (post Pass 35 / PR #23, working tree clean).

Findings #1, #2, and #4–#8 from the original audit were actioned on `chore/cleanup-backlog-fixes` — see [CLEANUP_LOG.md Pass 36](CLEANUP_LOG.md) for what changed. Only the finding below remains open. It's split into one branch per screen rather than a single `refactor/split-oversized-screens` branch — each screen is an independent risk surface, gets its own manual test pass, and merges on its own so partial progress is never left uncommitted.

**Disposal:** once actioned, fold the result into a new numbered entry in `CLEANUP_LOG.md` and delete this file — it's a working list, not a permanent doc.

---

## Complexity & Pattern Health

### Five screens are single oversized composables with no internal extraction
Well past the ~150-line guideline, comparable in scope to the `EarnItApp.kt` split done in Pass 10. Extraction pattern: pull screen-only pieces into `private` composables in the same file (matches the existing convention in `DataScreen.kt`/`TaskEditScreen.kt`), no new files/folders, no behavior changes — pure structural split, verified via ktlint + `assembleDebug` + `./gradlew test` + a manual exploratory pass per screen.

| File | Size | Status |
|---|---|---|
| `RewardDetailScreen.kt` | was 658 lines, one composable | **Done** on `refactor/split-reward-detail-screen`, merged via PR #25 — split into a ~130-line orchestrator plus 6 private helpers. ktlint/build/unit tests pass. |
| `TaskEditScreen.kt` | was ~637 lines in `TaskEditScreen(...)` before `EmojiPickerDialog`/`SliderRow` helpers | **Done** on `refactor/split-task-edit-screen` — split into a ~200-line orchestrator plus 7 private helpers. Added `TaskEditScreenUiTest` (9 instrumented tests) closing a coverage gap; full suite (63 tests) passing. See [CLEANUP_LOG.md](CLEANUP_LOG.md) Pass 37–38 for detail. |
| `SettingsScreen.kt` | was ~430 lines in `SettingsScreen(...)` before the first helper | **Done** on `refactor/split-settings-screen` — split into a ~60-line orchestrator plus 8 private section/card helpers. Added `SettingsScreenUiTest` (10 instrumented tests) closing a coverage gap; full suite (73 tests) passing. See [CLEANUP_LOG.md](CLEANUP_LOG.md) Pass 39 for detail. |
| `RewardEditScreen.kt` | was ~420 lines, one composable | **Done** on `refactor/split-reward-edit-screen` — split into a ~290-line orchestrator plus 7 private helpers. Added `RewardEditScreenUiTest` (10 instrumented tests) closing a coverage gap and pinning a pre-existing bug found along the way (see Correctness section below). See [CLEANUP_LOG.md](CLEANUP_LOG.md) Pass 41 for detail. |
| `HomeScreen.kt` | ~320 lines in `HomeScreen(...)` | Planned — `refactor/split-home-screen` |

Not a new finding, just a size note: `DataScreen.kt` has grown to 398 lines (main composable ~263) since Pass 33's "~305 lines" mention — still only `NudgeDebugCard` extracted, consistent with that pass's prior review. No action implied unless it keeps growing.

---

## Dead Code & Hygiene

### `optimalRewardCount` is a stored setting with no consuming logic anywhere
**Done** on `chore/remove-optimal-reward-count`, merged via PR #29 — see [CLEANUP_LOG.md](CLEANUP_LOG.md) Pass 40. Removed entirely; `maxRewardCount` redesigned as a 1–10 slider instead of a text field.

---

## Correctness

### Adding two new tasks in a row to an unsaved reward silently drops the first one
Found while writing `RewardEditScreenUiTest` on `refactor/split-reward-edit-screen` (see [CLEANUP_LOG.md Pass 41](CLEANUP_LOG.md)), pre-existing — not introduced by that branch's split (the `LaunchedEffect` logic was copied verbatim). On a brand-new, not-yet-saved reward, tapping "Add task" → "Create your own" twice in a row (creating and saving two separate new tasks before saving the reward itself) results in only the *second* task staying included — the first silently disappears from the list.

**Root cause:** `RewardEditScreen.kt`'s `taskState`/`taskStateReady` are plain `remember`, not `rememberSaveable`. Each "Create your own" round-trip navigates to `TaskEditScreen` and back, and Navigation Compose disposes `RewardEditScreen`'s composition while it's off the back stack — so both reset. The startup `LaunchedEffect` then re-derives every task's inclusion from the (still-unsaved, `cur == null`) reward, defaulting everything — including the task the user already added — back to not-included. Only the *current* `pendingTaskId` (which does survive, via `rememberSaveable`) gets explicitly re-included afterward, so each new "create new task" round-trip clobbers whichever task was added before it.

**Current status:** pinned, not fixed. `RewardEditScreenUiTest.sequentialCreateNewTasks_onUnsavedReward_onlyLastOneStaysIncluded` documents today's actual (buggy) behavior as a red-test-in-waiting rather than leaving it silently uncovered. Fixing it properly likely means making `taskState` survive the round-trip (e.g. a custom `Saver`, or restructuring so the reward is saved as a draft before the first task is added) — a real behavior change, out of scope for a structural-split branch.

**Workaround for users today:** add existing tasks via the "select from list" checkbox in `AddTaskToRewardDialog` (which doesn't leave the screen, so isn't affected) instead of repeated "Create your own" taps; or save the reward once a task list already exists before adding more via "Create your own".

---

## Test Coverage

Found during a full audit of unit/instrumented coverage against `TESTING.md`'s "Not Covered by Automated Tests" section (see [CLEANUP_LOG.md Pass 39](CLEANUP_LOG.md)), not part of the original whole-repo audit above. All three items below were previously accepted as untested; closer look shows all three are actually automatable now.

### Logging against an archived reward has no repository guard, and no test
`EarnItRepository.logCompletion(task, rewardId, detail)` (`EarnItRepository.kt:130`) never checks whether the reward is archived — it just inserts. Trivially testable today with no new dependencies: a `RoomIntegrationBase`-style test (same pattern as `HappyPathTest`) archives a reward, calls `logCompletion` directly against it (bypassing the UI, which is the whole point), and asserts what happens. Either documents the current orphaned-log behavior as accepted, or becomes the red test for a guard if one is wanted.

### Rapid double-tap logging — likely a real gap, not just a coverage gap
`EarnItViewModel.logTask()` (`EarnItViewModel.kt:166`) is `viewModelScope.launch { repository.logCompletion(...) }` with no loggability re-check before the write. `TESTING.md`'s current framing ("DAO writes are serialized, button re-evaluates after each Flow emission") explains why concurrent writes don't corrupt each other, but doesn't actually prevent two `logTask()` calls for the same non-repeatable task both succeeding before the Flow emission disables the button. Not testable via real screen-tap timing (Compose's `performClick()` waits for idle by design, which defeats the exact race being tested) — instead, a repository-level test launching two concurrent `logCompletion` calls for the same non-repeatable `(taskId, rewardId)` and asserting the resulting log count tests the real invariant directly. Expected result going in: 2 logs get written, meaning the risk is real, not just theoretical — worth confirming either way before deciding if a guard is warranted.

### True process death and restore — automatable with one new test dependency
No `androidx.test.uiautomator` dependency currently exists in `app/build.gradle.kts`. Adding it (same category as `androidx.work:work-testing` added in Pass 33) enables `UiDevice.executeShellCommand("am force-stop com.earnit.app")` — the identical command `MANUAL_TEST_PLAN.md` step 6 already has a human run manually, now scriptable. Caveat to keep in the test/doc once written: `am force-stop` is a harder kill than the OS reclaiming memory while the app sits in Recents — it also discards the saved-instance-state Bundle, so it robustly proves "cold start, state must come from Room/DataStore" but doesn't necessarily reproduce the softer "swiped away, Bundle preserved" scenario `TESTING.md`'s existing note describes ("user returns from Recents"). Worth being explicit about which of the two the new test actually proves.

### Cancel/dismiss buttons are untested across the entire app, not just one screen
Found while extending `RewardEditScreenUiTest` on `refactor/split-reward-edit-screen` (see [CLEANUP_LOG.md Pass 41](CLEANUP_LOG.md)) — checked whether Reward Edit's Cancel button specifically was worth covering, and found it's part of a much wider, consistent gap: every screen's Cancel button and every dialog's dismiss button (dialog Cancel/backdrop-dismiss included) across the whole app is untested. Each one is a one-line `popBackStack()`/`onDismiss()` callback with no logic, which is why none have been covered so far — but "untested everywhere" is itself worth deciding on deliberately rather than by accretion. Not actioned here since closing it means a cross-cutting pass touching most UI test files at once, not a fix scoped to one screen. Options if picked up: a shared parametrized test helper, or accept the current risk explicitly in `TESTING.md` with a stated rationale (matching the `AddTaskToRewardDialog` group-view precedent) instead of leaving it merely unmentioned.

---

## Reviewed with no findings (from the original audit, still accurate)

- **ktlint** — clean repo-wide (`./gradlew ktlintCheck` passed with zero violations at audit time).
- **Decoupling** — no ViewModel references to UI types, no Repository/Dao references to ViewModel/UI concerns.
- **State correctness** — `remember` vs `rememberSaveable` usage is correct throughout (all form-field state is saveable; plain `remember` uses are all derived/computed values). `LaunchedEffect` keys are all correctly scoped.
- **Dead code** — no TODO/FIXME comments, no commented-out code, no unused `Strings.kt` constants, no unused drawables, no unused private functions anywhere in `app/src/main/java/com/earnit/app`.
- **Naming consistency** — all screen files follow `*Screen.kt` convention and sit in the right package; no stdlib/Compose name shadowing.
- **Spec Screen Map (§10)** — matches the actual nav graph and screens; no drift.
- **Tip Jar deferred-idea status** — `FeatureFlags.TIP_JAR_ENABLED = false` matches the spec's "UI complete, hidden" description; `MockTipRepository`/`TipViewModel`/gated `AboutScreen` section all present as described.
- **`DEV_PLAYBOOK.md §4 Known Limitations`** — every item spot-checked and still accurate: widget colors, the four named hardcoded hex values (progress track, dividers, activity-log task name, reward cost label), and `taskState`/`rewardLinkState` still using plain `remember`.
- **`DEV_PLAYBOOK.md §2 Ship Checklist`** — unchanged, still fully open (store description, data-safety form, closed-testing guide share, CI badge/README, test-artifact publishing, AAB auto-upload stretch goal). No action implied here — just confirmed nothing was silently completed without the checklist being struck through.
- **`MANUAL_TEST_PLAN.md`** — correctly reflects the Pass 35 gesture-based dev-mode unlock; no stale reference to the old 7-tap About-screen trigger.
- **Other `TESTING.md` Deferrals** — none appear closed by recent commits (last 15 are all nudge-feature and dev-unlock-gesture work; none touch widget-refresh injection, RevenueCat, group-view UI, transaction rollback testing, or FK cascade coverage).
