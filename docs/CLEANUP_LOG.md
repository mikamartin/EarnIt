# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 62 — `fix/reward-detail-completion-icons` branch

Bug: `RewardDetailScreen`'s mandatory-task star and repeatable-task icon reused the same shape+color language for two different meanings depending on screen. On `RewardEditScreen` filled/primary means "flag is on"; on `RewardDetailScreen` the same filled/primary-vs-outlined/dimmed swap instead encoded "logged this cycle," so a mandatory task that simply hadn't been logged yet displayed as a faint, easy-to-misread star — surfaced when a genuinely mandatory, already-logged task's star still read as dim. Reworked the "Complete to earn points" row to checkmark (completion) + always-solid ★/↻ (flags), so the two concerns are visually independent; applied the same "flags never dim" fix to the reward header's per-task stars; removed the near-duplicate, unlabelled version of the same star row from the `HomeScreen` reward card entirely rather than fixing it in place, since a bare star with no attached task name adds no information there. Also fixed the repeatable-flag icon's tint (`secondary` → `primary`, matching `RewardEditScreen`'s own convention) and bumped its size 14dp → 16dp, since `Icons.Default.Refresh`'s thin-stroke glyph reads visually lighter than the solid `Star`/`CheckCircle` icons beside it even at identical color and opacity.

#### Duplication ✅ — found and fixed
Extracted `RewardProgress.isTaskLogged(taskId)` (`Entities.kt`), replacing the inline `activeLogs.any { it.taskId == task.id }` in `RewardDetailScreen.kt`'s task row *and* the near-identical expression already inside `canClaim`'s own definition in the same class, so both now share one implementation. Considered also routing `EarnItWidget.kt`'s two identical `activeLogs.any { it.taskId == mt.id }` checks (its own mandatory-task display) through the same helper — left alone: that file isn't otherwise touched by this branch, and folding an unrelated file into an icon-behavior fix risks scope creep. Noted here as a followup opportunity, not done.

#### Decoupling ✅ (n/a)
No ViewModel, Repository, or Dao logic touched.

#### Complexity & Pattern Health ✅ (checked)
`RewardDetailScreen.kt`'s task-row composable stayed roughly the same size/shape — reordered branches, not new nesting. No M3-provided component reimplemented; checkmark/star/refresh are plain `Icon` calls, same idiom as before and as `SharedDialogs.kt`.

#### Naming Consistency ✅ (checked)
`isTaskLogged` matches the existing `canClaim`/`showsProgressNumbers` boolean-property convention on the same class. `REWARD_TASK_DONE_DESC`/`REWARD_TASK_NOT_DONE_DESC` follow the existing `REWARD_*_DESC` naming pattern in `Strings.kt`.

#### Hardcoded Values ✅ (n/a)
Reused existing theme colors (`colorScheme.primary`, `onSurfaceVariant.copy(alpha = 0.45f)` — the same alpha the old dimmed star already used) and the existing 14dp/16dp size scale — no new magic numbers.

#### Accessibility ✅ — found and fixed
Added `contentDescription` to the checkmark (`REWARD_TASK_DONE_DESC`/`_NOT_DONE_DESC`, new strings) and reused the existing `REWARD_MANDATORY_DESC`/`REWARD_REPEATABLE_DESC` for the task-row flag icons (previously all three were `null`). Left the reward header's per-task stars at `contentDescription = null` deliberately — they're decorative (several identical stars with no task name attached convey nothing extra to TalkBack beyond the reward name already announced; a description would just repeat "Mandatory" N times with no context). No new independently-tappable icon targets, so the 48dp tap-target item doesn't apply.

#### Deprecated APIs ✅ (n/a)
`Icons.Default.CheckCircle`/`Icons.Outlined.CheckCircle` are current, non-deprecated symbols already available via the existing `material-icons-extended` dependency.

#### Spec Review ✅ — found and fixed
Added a line to `EARNIT_SPEC.md`'s Gatekeeper Logic section documenting the checkmark + static-flag row behavior (previously undocumented). Grepped the rest of the doc for stale references afterward — the widget's own "★" mention (line 193, widget hint copy) is unrelated prose about a file this branch doesn't touch, so left as-is.

#### Tests ✅ (1 new file, 1 existing file extended)
- `RewardProgressTest.kt`: 3 new cases for `isTaskLogged` (match found / no match / distinguishes between tasks when multiple logs exist) — extended in place rather than a new file, since it's a single new property on the same `RewardProgress` class the file already covers exhaustively, not a new cohesive area of behavior.
- `RewardDetailTaskRowUiTest.kt` (new, 2 tests): flags stay visible and unchanged across the logged/not-logged transition; both flag icons are absent when neither applies. Warrants its own file — new composable-level behavior, not an extension of an existing class's premise.
- Ran the new class plus every Reward-related UI test class on a connected emulator (API 36): all passed, 110/110 instrumented tests total. A physical device connected in parallel failed the majority of the suite wholesale with `IllegalStateException: No compose hierarchies found in the app` (activity never launched) — a device/OS-level issue, not a real failure: it hit test classes this branch never touched (`CleanUpScreenUiTest`, `DragReorderUiTest`, etc.) just as often as the ones it did. Disregarded per direction.
- `TESTING.md`: updated `RewardProgressTest`'s row count (18→21) and description, added the new `RewardDetailTaskRowUiTest` row, and updated the pyramid/section-header/cadence-table aggregate counts (Unit 182→185, instrumented 108→110 — both exact; UI pyramid figure rounded 76→75 per the doc's own stated rounding policy for aggregates, Integration's unrelated 34 left as-is since it didn't change).
- `MANUAL_TEST_PLAN.md`: checked, no update needed — this row's rendering doesn't cross a system-process boundary; it's fully driven by the new instrumented test.
- No `AppModule`/`TestAppModule`/`@Inject` changes, but ran `./gradlew assembleDebugAndroidTest` anyway since a new instrumented test file was added — passed, confirming the new test actually compiles rather than trusting Gradle's incremental build to have caught a compile error.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`.

---

### Pass 63 — `fix/widget-progress-bar-clipping` branch

Bug: the widget's mandatory-task and all-tasks-done hints rendered as a second text line under the reward name in `StandardContent` (`EarnItWidget.kt`) — a fixed-height `Column` centered inside a fixed-size `Box` with no scroll/shrink fallback (`fea294c`; see this file's own known-limitations entry in `DEV_PLAYBOOK.md`). The extra line's height could exceed the widget's granted box on short or resized widgets, silently clipping the progress bar below it. `fix/widget-hint-overflow` had already bounded one trigger of this (the hint text wrapping to a 2nd line on narrow widths, via `maxLines = 1`) but not the height the hint's own single line adds in the first place. Replaced both hint text lines with a single exclamation-mark icon (`ic_alert.xml`), tinted with the same `notification` accent `RewardDetailScreen` already uses for this exact hint copy (`accents.notification` — red on Warm Gold/Forest, amber on Ocean Blue), inline next to the reward name — same row, no added height — carrying the explanation via `contentDescription` instead of visible text. `WidgetColors` didn't expose that accent before; added a `notification` field, sourced from `ColorSchemes.accents(scheme)` (a single value per color scheme, not light/dark-variant like the rest of `WidgetColors`). Several iteration issues along the way, each caught only once actually seen rendered rather than reasoned about in code:
1. First pass tinted the icon `colors.onSurfaceVar` (muted), then `colors.primary` — both read as too weak/not urgent enough. Matching `RewardDetailScreen`'s own notification color for the identical hint text fixed both the visibility complaint and a cross-screen consistency gap.
2. The icon started as a Material "error"-style glyph — a *filled* circle with an exclamation mark inside. Tinted as a single flat color, the filled circle and the mark inside it were the same solid color with no contrast, so the mark nearly disappeared at widget scale. First fix removed the circle entirely (bare bar+dot mark, no enclosure), widening the glyph to use more of its 24×24 viewport in the process — the initial mark-only attempt occupied only a ~3.5-unit-wide sliver, reading thin even before the circle problem. Shape then went through several more rendered-and-reviewed rounds: restored a circle as a hollow *stroke* ring (not a fill) around the mark, since `ColorFilter.tint()` renders both in one uniform color but the ring's hollow interior stays transparent, so ring and mark read as two distinct shapes rather than merging into one flat disc; bumped the rendered size 18dp→22dp and thickened both the ring stroke and the mark itself for boldness; then shrunk and re-centered the mark twice more to open up clear padding between the mark and the ring's inner edge, which the size/weight bump had eaten into. `ic_alert.xml`'s final geometry: a `strokeWidth="2.4"` ring at r=9 (centered in the 24×24 viewport), with the mark's bar and dot kept within roughly r≈5, leaving a visible transparent gap between them.
3. First pass placed the icon far from the name: giving the reward-name `Text` its own `defaultWeight()` (`Dimension.Expand`) stretched its box to fill the whole row, so the icon (unweighted, positioned after) landed at the row's far edge instead of right after the rendered glyphs. Fixed by removing the weight from the `Text` — only the wrapping `Row` carries it — so the name hugs its own content and the icon sits immediately after it, while a long name is still bounded by the `Row`'s weighted allocation rather than pushing the action button off-widget.

#### Duplication ✅ — found and fixed
The two hint blocks (mandatory-task / all-tasks-done) were identical apart from their text and test tag. Extracted a private `HintIcon(hintText, tag, colors)` composable, replacing both inline blocks.

#### Decoupling ✅ (n/a)
No ViewModel, Repository, or Dao touched — purely the widget's Glance layer.

#### Complexity & Pattern Health ✅ (checked)
`StandardContent`'s name/hint row is simpler after this change (one `Row`, not a `Column` wrapping a conditional second `Text`), not more complex. The new `HintIcon` composable has 2 callers, earning its extraction (see Duplication).

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` (unused-import enforcement) passes. `git status` confirms exactly the intended files changed, no stray untracked files.

#### Naming Consistency ✅ (checked)
`HintIcon` follows the file's existing composable naming (`ProgressBar`, `FlashContent`, `ClaimedState`, ...). `ic_alert.xml` matches the existing `ic_add.xml`/`ic_trophy.xml` drawable naming convention.

#### Hardcoded Values ✅ (checked, consistent with existing convention)
`ic_alert.xml`'s placeholder `fillColor="#FFFBF0"` is overridden at runtime via `ColorFilter.tint(colors.notification)` — the identical pattern `ic_add.xml` already uses, not a new inconsistency.

#### Accessibility ✅ — the point of this fix
The hint's explanation moves from always-visible text to an icon's `contentDescription` — same information exposed, now decoupled from the layout height it used to cost. The icon carries no independent click action (the whole widget body already routes to the app), so the 48dp tap-target item doesn't apply.

#### Deprecated APIs ✅ (n/a)

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md`'s widget Display States section described both hint states as a "subtitle below the reward name" with slightly stale literal copy (the quoted strings had already drifted from the actual `Strings.kt` constants, unrelated to this change) — rewrote both bullets to describe the icon + content-description behavior and corrected the quoted text while in there.

#### Tests ✅ (1 new file, 1 existing file extended; 1 pre-existing gap found and closed)
- `WidgetContentTest.kt`: both hint-existence assertions (`standardContent_mandatoryTaskUnloggedButPointsMet_showsHint`, `standardContent_allTasksDoneBelowCost_showsDisabledLogButtonAndHint`) extended with `assertHasContentDescriptionEqualTo` to verify the explanation actually reaches the icon, not just that a node with the right test tag exists.
- The icon-adjacency bug itself (the `Text` `defaultWeight()` mistake) has no automated regression test — checked `glance-testing`'s unit-test filters (`UnitTestFilters.kt`) directly and confirmed they only cover `testTag`/`contentDescription`/`text`/click-action, nothing for layout, width, weight, or sibling position. Not automatable with the current tooling; covered instead by the `MANUAL_TEST_PLAN.md` step 8 update below, which now explicitly calls out checking that the icon lands next to the name rather than at the row's far edge.
- Found and closed a pre-existing gap while adding the `notification` field: `widgetColors()` (scheme + light/dark mode → `WidgetColors`) had zero test coverage — `WidgetContentTest` always builds its own hardcoded `WidgetColors` fixture and passes it straight to `StandardContent`, never calling `widgetColors()` itself. True for every field before this pass, not just the new one, but this pass is what extended that already-untested function. Changed `widgetColors()` from `private` to `internal` (same rationale as the existing `StandardContent`/`WidgetColors` precedent) and added `WidgetColorsTest.kt` (4 tests): `notification` matches `ColorSchemes.accents()` for Warm Gold and Forest, diverges to amber for Ocean Blue (the one scheme where it actually differs — the case most likely to silently regress), and dark mode changes `primary` from its light-mode value for the same scheme (via Robolectric `RuntimeEnvironment.setQualifiers("+night")`), confirming the light/dark branch itself runs rather than just compiling.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass. Ran `assembleDebugAndroidTest` too even with no `AppModule`/`TestAppModule`/`@Inject` change, since a new drawable resource and widget composable restructuring are easy to get wrong in ways only a real compile catches.
- `TESTING.md`: added the `WidgetColorsTest` row and updated the unit-test aggregate counts (185→189, pyramid + section header + cadence table, all exact).
- `DEV_PLAYBOOK.md`'s known-limitations entry for `StandardContent` updated to record this as a second, differently-triggered instance of the same underlying architectural gap (no shrink/scroll fallback), not a fix of the gap itself — a longer reward name at large accessibility font scale would still reproduce the same clipping.
- `MANUAL_TEST_PLAN.md` steps 8, 9, and 14 updated to describe the icon instead of the old hint-text-subtitle wording.
- This branch forked before `fix/reward-detail-completion-icons` (PR #65) merged to `main`. Rebased onto the merged `main` before finishing this pass (stash → reset → pop, clean auto-merge) so `docs/CLEANUP_LOG.md`'s already-trimmed history and `TESTING.md`'s already-updated counts from that PR weren't reintroduced or duplicated.

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
