# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 57 — `fix/widget-screens-cutout-inset-overlap` branch

Bug report: opening the log picker from the home-screen widget put the reward title under the phone's front-camera cutout. Root cause: `WidgetTaskLogActivity` and `WidgetConfigActivity` each set content on a bare `Surface` with no inset handling at all — no `enableEdgeToEdge()`, no `Scaffold`, no `windowInsetsPadding` — unlike `MainActivity`, which calls `enableEdgeToEdge()` and routes all its screens through `Scaffold` (insets arrive as content padding automatically). Since `compileSdk`/`targetSdk` is 36, edge-to-edge is enforced for every activity regardless of opt-in, so both widget activities were always drawing behind the status bar/cutout — only became visible once content reached the top of the screen. Fixed by adding `.windowInsetsPadding(WindowInsets.safeDrawing)` to the root `Surface` in both `ThemedTaskPicker` (`WidgetTaskLogActivity.kt`) and `ThemedWidgetConfig` (`WidgetConfigActivity.kt`).

#### Duplication ✅ (checked, left inline)
The same two-line modifier chain (`fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)`) now appears in both files. Considered extracting a shared wrapper composable/modifier for it: rejected — it's a standard Compose insets idiom, not a design-system value (color/spacing/shape) with an existing home to live in, and the two `Surface` blocks otherwise wrap unrelated content in files with no existing shared UI file between them. Two lines duplicated twice is clearer than a one-off abstraction for this.

#### Decoupling / Complexity & Pattern Health / Naming Consistency / Accessibility ✅ (n/a)
No ViewModel, Repository, or Dao touched; no new files, composables, or symbols added; no new tappable targets.

#### Dead Code & Hygiene ✅
`git status`/`git diff --stat` confirm exactly the 2 intended Kotlin files changed, plus the two doc files below — nothing stray.

#### Hardcoded Values ✅ — the point of this fix
Deliberately used `WindowInsets.safeDrawing` (status bar + nav bar + display cutout, all sides) rather than a fixed `Modifier.padding(top = Xdp)` — a hardcoded value would be wrong on any device with a differently sized cutout or none at all.

#### Deprecated APIs ✅ (checked)
`windowInsetsPadding`/`WindowInsets.safeDrawing` are current, non-deprecated Compose Foundation APIs — no overlap with the deprecated `Window.setStatusBarColor`-family calls Pass 56 addressed.

#### Spec Review ✅ (checked, no changes needed)
Grepped `EARNIT_SPEC.md` for `WidgetTaskLogActivity|WidgetConfigActivity` — both are described in terms of user-visible flow (reward/task selection, theming, confirmation), not layout/inset detail. This is a rendering-correctness fix, not a behavior change, so nothing to reconcile.

#### Tests ✅ (0 new automated tests — added to MANUAL_TEST_PLAN.md instead)
- Display-cutout overlap only reproduces on a real device with a physical cutout; Compose UI tests (Robolectric or emulator without a cutout) can't observe it, matching the existing widget-activity-chain rationale in `MANUAL_TEST_PLAN.md`. Added a step to the existing "Widget full flow" section instead of a new entry, since it exercises the same two activities already covered there.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`.
- `TESTING.md`: no changes needed — no test added, removed, or renamed.

---

### Pass 58 — `fix/widget-all-tasks-done-state` branch

Bug report: on the Rewards screen, a reward with all one-time tasks done and no repeatable tasks shows a disabled `+ LOG` pill plus an explanatory hint (`REWARD_ALL_TASKS_LOGGED_HINT`); the equivalent widget state showed nothing at all — no button, no text. Root cause: `widgetActionButtonFor`'s (`WidgetActionButton.kt`) 4-way `when` had an `else -> NONE` branch that, given the other three branches' conditions, could only ever be reached by this exact state (all tasks done, nothing repeatable) — and `EarnItWidget.kt` rendered `NONE` as `{}`. Fixed by renaming `NONE` to `LOG_DISABLED`, rendering it as a muted, non-clickable version of the `+ LOG` button, and adding a matching one-line hint (`WIDGET_ALL_TASKS_LOGGED_HINT`, "All tasks done — add more") below the reward name — mirroring the existing `showMandatoryHint`/`WIDGET_MANDATORY_HINT` pattern, and confirmed mutually exclusive with it (both hints can never be true for the same render, so they never contend for the same line).

#### Duplication ✅ — found and fixed
`widgetActionButtonFor` reimplemented `RewardProgress.loggableTasks`' exact filter inline (`completedIds`/`taskRefsMap`/`hasTasks` local vars duplicating `Entities.kt`'s `loggableTasks` getter). Since this pass was already rewriting that function, simplified it to call `progress.loggableTasks.isNotEmpty()` and `progress.allTasks.isEmpty()` directly — removes the duplicate logic and shortens the function to a single `when` expression. Re-ran `ktlintCheck` and `test` after this refactor to confirm behavior didn't shift.

#### Decoupling / Complexity & Pattern Health / Naming Consistency ✅ (checked)
No ViewModel/Repository/Dao touched. `LOG_DISABLED`'s render branch reuses the existing `LOG` button's shape/sizing (32dp circle) rather than introducing a new layout pattern; only the color source and click-wiring differ.

#### Hardcoded Values ✅ (checked)
New muted-button colors resolve from `WidgetColors.track`/`onSurfaceVar` (existing theme-aware `ColorProvider`s), not literal hex values — consistent with every other widget button state.

#### Accessibility ✅ (checked)
`LOG_DISABLED`'s icon `Image` carries a non-empty `contentDescription` (reusing the hint string), unlike being decorative/`null`, since the box has no visible label text of its own the way `CLAIM`/`ADD_TASK` do.

#### Deprecated APIs ✅ (n/a)
No new API surface touched beyond existing Glance `Image`/`ColorFilter.tint`, both current.

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md`'s Widget Display States table documented the *buggy* behavior as intentional ("All tasks done, points still short: reward name and progress bar only; no button shown"). Corrected to describe the disabled button + hint.

#### Tests ✅ (0 new files; 2 existing tests corrected, both previously asserting the bug)
- `WidgetActionButtonTest`'s `` `non-repeatable task already logged and points below cost returns NONE` `` renamed and updated to assert `LOG_DISABLED` — this test was passing *because* it encoded the bug, not despite it.
- `WidgetContentTest`'s `standardContent_allTasksDoneBelowCost_showsNoActionButton` renamed to `standardContent_allTasksDoneBelowCost_showsDisabledLogButtonAndHint`, extended to assert the disabled button exists with `assertHasNoClickAction()` and the hint text renders.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`.
- `TESTING.md`: updated the `WidgetActionButtonTest`/`WidgetContentTest` table rows and the "Widget action-button selection" prose section to say `LOG_DISABLED` instead of `NONE` and describe the new disabled-button-plus-hint assertion. Counts unchanged (both are renamed/extended existing tests, not new files).
- `MANUAL_TEST_PLAN.md`: step 9 (all-tasks-done widget state) updated to describe the disabled button and hint instead of "no button is shown"; step 14 (narrow-width/minimal-footprint overflow check) extended to also cover this new hint, since it shares the exact clipping risk `fix/widget-hint-overflow` already found for the mandatory-task hint.

---

### Pass 59 — `fix/widget-all-tasks-done-state` branch

Product change: new task/reward links now default to repeatable (`isRepeatable = true`) instead of one-time, since most real tasks (chores, habits) are repeatable and one-time tasks are the less common case. `TaskEditState`'s constructor default (`SharedDialogs.kt`) is the single UI-state source most of these flows read from, so flipping it there covers: creating a brand-new task with no reward link yet, per-row defaults in `AddTaskToRewardDialog` for tasks not yet flagged, and `RewardEditScreen`'s auto-include-newly-created-task and fallback-read paths. Three other sites don't derive from that class and needed their own flip: `RewardEditScreen.kt`'s two `?.isRepeatable ?: false` fallbacks (rendering a not-yet-linked task's row in the reward's task-selection list) → `?: true`; `TaskEditScreen.kt`'s hardcoded `Pair(false, false)` link flags for the "create new task" shortcut launched from `AddTaskToRewardDialog` (no UI toggle exists on that path) → `Pair(false, true)`; and `EarnItRepository.addTaskToReward`'s default parameter, flipped for signature-accuracy even though every call site already passes explicit args (verified via grep — dead default, zero behavioral effect). Deliberately left `RewardTaskCrossRef`'s own entity-level constructor default (`Entities.kt`) and the vestigial, UI-disconnected `TaskEntity.repeatable` field untouched — see Duplication below.

#### Duplication ✅ (checked, one default deliberately left alone)
Considered also flipping `RewardTaskCrossRef.isRepeatable`'s entity-level default (currently `false`) for full consistency. Grepped every constructor call site across `main` and `test`: all of them (`EarnItRepository`, `TestDataSeeder`, and every unit test building a `RewardTaskCrossRef` directly) pass `isRepeatable` explicitly — except one, `GatekeeperTest.kt:20`'s `ref(taskId)` helper, which omits it and relies on the constructor default for mandatory-task-gating fixtures. Flipping the entity default would have silently turned every `GatekeeperTest` fixture repeatable, changing `canClaim`-gating test semantics for a reason unrelated to this pass. Left it at `false` — nothing in production reads it, so there's no behavioral inconsistency, only an unused constructor default that no longer matches the new UI-level house default. `TaskEntity.repeatable` (already `true`, no UI toggle anywhere) is unaffected either way.

#### Decoupling / Naming Consistency / Hardcoded Values / Deprecated APIs ✅ (n/a)
No ViewModel/Repository logic relocated, no new symbols, no hardcoded colors/dimensions, no new or deprecated API surface — every change is a boolean default flip at an existing site.

#### Complexity & Pattern Health ✅ (checked)
No new composables, branches, or abstractions — same `TaskEditState`/`Pair`/default-parameter shapes as before, only the literal values changed.

#### Accessibility ✅ (n/a)
No new tappable targets or content descriptions — the existing mandatory-star/repeatable-refresh toggle icons and their content descriptions (`REWARD_REPEATABLE_DESC`/`REWARD_NOT_REPEATABLE_DESC`, `TASK_REPEATABLE_DESC`/`TASK_ONCE_DESC`) are unchanged; only which one shows first differs.

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md`'s Task Links (`RewardTaskCrossRef`) section documented what the `isMandatory`/`isRepeatable` flags mean but not their default for a new link. Added a line stating the new default and the reasoning (most tasks are repeatable in practice, so the toggle starts in the common state).

#### Tests ✅ (0 new files; 3 existing UI tests corrected, one for the wrong reason it was passing)
- `RewardEditScreenUiTest.taskRow_mandatoryRepeatableTogglesAndUncheckRemoves`: the newly-auto-included task now shows `REWARD_REPEATABLE_DESC` (not `REWARD_NOT_REPEATABLE_DESC`) by default; the toggle-click assertions swapped direction to match (click repeatable → not-repeatable, instead of the reverse).
- `TaskEditScreenUiTest.rewardLinks_checkboxAndMandatoryRepeatableToggles`: same swap for the not-yet-linked task's disabled toggle state and the enabled-after-checking assertion; the uncheck-resets-to-not-repeatable ending assertion needed no change, since `withIncludedSetTo(false)` always resets to `false` regardless of the class default.
- `RewardAllTasksLoggedHintUiTest.createRewardWithOneLoggedTask` (helper used by both tests in the class): this class's entire premise is a non-repeatable task exhausting `loggableTasks` after one log — now that `AddTaskToRewardDialog` defaults new links to repeatable, the helper explicitly clicks the repeat toggle off before confirming the add, instead of relying on the default. Without this fix both tests in the class would have failed for a reason unrelated to what they're testing.
- Ran all three affected classes on the connected emulator (`RewardEditScreenUiTest`, `TaskEditScreenUiTest`, `RewardAllTasksLoggedHintUiTest` — 26 tests total) — all passed after the fixes above.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't strictly required, but ran it anyway (and it passed) since `androidTest` sources were touched.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`.
- `TESTING.md`: checked every row describing the three affected test classes — none asserted a specific default value in prose (only behavior like "toggles flip their description"), so no wording was stale; no changes needed.
- `MANUAL_TEST_PLAN.md`: step 9 (widget's all-tasks-done state, added in Pass 58) required a non-repeatable task to set up — added a note that a tester must now explicitly toggle a task to non-repeatable when linking it, since that's no longer the default.
