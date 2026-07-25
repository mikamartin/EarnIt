# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

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

---

### Pass 60 — `fix/radio-group-talkback-semantics` branch

Code review comment on an unrelated PR flagged that every radio-option row in the app used `Modifier.clickable` with `RadioButton(onClick = null)` inside it, instead of `Modifier.selectable(role = Role.RadioButton)` — meaning TalkBack announces these as generic tappable rows, not radio buttons with selected state or group position. Confirmed via grep this was a real app-wide pattern, not isolated to one screen: the shared `RadioRow` composable (`EarnItButtons.kt`, used by `TaskEditScreen.kt`'s group picker and `TasksScreen.kt`'s `LogForRewardDialog` reward picker) and `SharedDialogs.kt`'s bespoke `LogTaskDialog` task-picker row both had it. Fixed all four call sites: `RadioRow` and the three bespoke rows now use `Modifier.selectable(selected, onClick, role = Role.RadioButton)`, and every list containing radio options (`TaskEditScreen.kt`'s group `Column`, `TasksScreen.kt`'s reward-picker `Column`, `SharedDialogs.kt`'s task-picker `LazyColumn`) now carries `Modifier.selectableGroup()`.

#### Duplication ✅ (checked, left inline)
The `.selectable(role = Role.RadioButton)` swap is now duplicated across 4 call sites (`RadioRow`, `TaskEditScreen.kt`'s custom-group row, `SharedDialogs.kt`'s task-picker row). Considered routing `SharedDialogs.kt`'s row through the shared `RadioRow` composable instead: rejected — that row has a trailing icon cluster (mandatory star, repeatable icon, points) after the label plus `Alignment.Top` instead of `CenterVertically` (so the cluster stays pinned to the top when a long task name wraps to two lines), neither of which `RadioRow`'s `label: String`-only API supports. Adding a trailing-content slot and configurable alignment to serve one caller would be new API surface for a cleanup pass about semantics parity, not deduplication — same reasoning as Pass 57's rejected extraction.

#### Decoupling ✅ (n/a)
No ViewModel, Repository, or Dao touched — purely Compose modifier/semantics changes.

#### Complexity & Pattern Health ✅ (checked)
No new composables or reimplemented M3 components — `RadioButton` usage itself is untouched; only the wrapping row's interaction modifier changed. `TaskEditScreen.kt`'s custom-group row (radio + inline `BasicTextField`) got the same `.selectable` swap despite the nested text field — verified on-device (see Tests) that `clickable`-family modifiers merge descendants but stop at nested interactive elements, so a fear that this would break the text field's own tap-to-position-cursor behavior turned out to be moot for the *other* three rows (no nested interactive children) and wasn't separately re-verified for this specific row's cursor-placement behavior beyond the existing test's `performTextInput` calls still passing.

#### Naming Consistency / Hardcoded Values / Deprecated APIs ✅ (n/a)
No new symbols, colors, dimensions, or new/deprecated API surface — `Modifier.selectable`/`selectableGroup` are current, non-deprecated Compose Foundation APIs.

#### Dead Code & Hygiene ✅
`git status`/`git diff --stat` confirm exactly the 7 intended source files changed, plus the two doc files. `ktlintCheck` (which enforces no-unused-imports) passes clean.

#### Accessibility ✅ — the point of this fix
This *is* the accessibility fix. No new tappable targets were added and no existing padding/size modifiers were touched, so the 48dp tap-target checklist item doesn't apply here.

#### Spec Review ✅ (checked, no changes needed)
Grepped `EARNIT_SPEC.md` for `radio`/`accessib`/`TalkBack` — the one hit (Task Edit section, describing the group picker's "filled-card radio-button list") is a user-visible-behavior description with no semantics-layer claim to reconcile; visible behavior is unchanged.

#### Tests ✅ (0 new files; 3 existing tests extended with semantics assertions)
- `TaskEditScreenUiTest.groupPicker_...`, `LogForRewardDialogUiTest.multiReward_...`, and `SharedDialogsCancelUiTest.logTaskDialog_cancel_...` each gained 1–2 assertion lines checking `Role.RadioButton` + `assertIsSelected()`/`assertIsNotSelected()` on the row(s) they already interact with — below the 3+-new-tests threshold for a new file, so extended in place per existing precedent.
- Verified empirically, not just reasoned about: before writing the assertions, decompiled Compose Foundation's `Clickable.kt` (the shared base `selectable`/`clickable` build on) to confirm `shouldMergeDescendantSemantics` is hardcoded `true`, meaning a row's label `Text` child merges its properties up into the row's own semantics node in the tree `onNodeWithText` queries by default.
- Ran the 3 touched classes on a connected emulator first (16/16 passed), then the full instrumented suite (108/108 passed) as a regression check given `RadioRow` is shared across multiple screens.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required (`connectedDebugAndroidTest`'s successful compile of the androidTest variant covers the same ground here).
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`.
- `TESTING.md`: updated the three affected table rows to mention the new semantics assertions, and added a new "Radio-group TalkBack semantics" entry under Edge Cases — Covered, since this is the app's first automated accessibility-semantics coverage and nothing referenced it before.
- `MANUAL_TEST_PLAN.md`: checked — no update needed. That doc is scoped to journeys crossing a real system-process boundary (file picker, widget activity chain, `WorkManager`); semantics-tree assertions are fully drivable in-process, which the passing instrumented tests above demonstrate directly rather than argue by analogy.
