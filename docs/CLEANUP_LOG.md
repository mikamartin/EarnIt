# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 61 — remove widget-log notification

Product decision: the system notification fired every time a task was logged from the home-screen widget ("Task name / Logged! +X pts") was noise — the widget's own flash + haptic already confirm the log in the moment, and the notification added a second, redundant confirmation for an event the user just triggered themselves. Removed `WidgetTaskLogActivity`'s `showNotification()`, its notification channel (`earnit_widget_log`), and its own `POST_NOTIFICATIONS` permission request (redundant — `MainActivity` already requests that permission on first launch for inactivity nudges). Inactivity-nudge notifications, which fire when *nothing* has been logged for 48h/96h, are untouched — different code path, different channel, different purpose (re-engagement vs. confirmation).

#### Duplication / Decoupling / Complexity & Pattern Health ✅ (n/a)
Pure deletion — no composables, ViewModel, Repository, or Dao touched, nothing to duplicate or extract.

#### Dead Code & Hygiene ✅ — found and fixed
Removed now-unused imports (`NotificationChannel`, `NotificationManager`, `PendingIntent`, `Intent`, `PackageManager`, `Build`, `ActivityResultContracts`, `NotificationCompat`, `NotificationManagerCompat`, `MainActivity`, `R`), the `WIDGET_LOG_CHANNEL_ID`/`WIDGET_LOG_NOTIF_ID` constants, and the orphaned `pts` local that only existed to feed the removed notification text. `ktlintCheck` (unused-import enforcement) passes clean. Also removed `Strings.widgetLoggedNotif()` and `WIDGET_NOTIF_CHANNEL_NAME`, now unreferenced. Checked `R.drawable.ic_add` (the notification's icon) is still used elsewhere (`NudgeWorker`, `EarnItWidget`) so it wasn't orphaned. `git status`/`git diff --stat` confirm exactly the 4 intended files changed. Considered whether the now-defunct `earnit_widget_log` notification channel needs explicit cleanup (`NotificationManager.deleteNotificationChannel`) for existing installs — not needed, `versionCode`/`versionName` (1 / "1.0") confirm the app hasn't shipped a release yet, so no device has ever created that channel.

#### Naming Consistency / Hardcoded Values / Accessibility / Deprecated APIs ✅ (n/a)
No new symbols, colors, dimensions, tappable targets, or API surface — everything here is removal of existing, non-deprecated code.

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md`: removed the notification from the "Task Logging from Widget" flow description and the "Celebratory Feedback" list, and reworded the nudge section's "unlike the silent widget-log channel" comparison (the widget-log channel no longer exists) to state plainly that nudges are now EarnIt's only notification channel. Grepped the whole doc for `notification`/`POST_NOTIFICATIONS`/`permission` afterward to confirm no other stale references remain — the one other hit ("Mascot Unlock Notifications") is an unrelated in-app badge/snackbar concept, not a system notification.

#### Tests ✅ (0 new files; 0 existing tests referenced the removed notification)
- Grepped `androidTest` and `test` source sets for `notif`/`Notification` — the only hits are `HiltTestRunner`'s generic `POST_NOTIFICATIONS` pre-grant (still needed, nudges still require it) and `MascotNotificationTest` (the unrelated in-app badge concept above). No test asserted the widget-log notification's content or channel, so none needed updating or deleting.
- `TESTING.md`: checked — the "Widget task logging" Deferrals entry and manual-only rationale don't call out the notification specifically, no change needed.
- `MANUAL_TEST_PLAN.md`: step 5 updated to drop "Confirm a notification appeared (...)" from the widget-logging journey.
- `AppModule`/`TestAppModule` untouched; no new `@Inject` site. Ran `./gradlew assembleDebugAndroidTest` anyway since `WidgetTaskLogActivity` (an existing `@AndroidEntryPoint`/`@Inject` class) was touched — passed.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`.

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

Bug: the widget's mandatory-task and all-tasks-done hints rendered as a second text line under the reward name in `StandardContent` (`EarnItWidget.kt`) — a fixed-height `Column` centered inside a fixed-size `Box` with no scroll/shrink fallback (`fea294c`; see this file's own known-limitations entry in `DEV_PLAYBOOK.md`). The extra line's height could exceed the widget's granted box on short or resized widgets, silently clipping the progress bar below it. `fix/widget-hint-overflow` had already bounded one trigger of this (the hint text wrapping to a 2nd line on narrow widths, via `maxLines = 1`) but not the height the hint's own single line adds in the first place. Replaced both hint text lines with a single small static icon (`ic_info.xml`) inline next to the reward name — same row, no added height — carrying the explanation via `contentDescription` instead of visible text.

#### Duplication ✅ — found and fixed
The two hint blocks (mandatory-task / all-tasks-done) were identical apart from their text and test tag. Extracted a private `HintIcon(hintText, tag, colors)` composable, replacing both inline blocks.

#### Decoupling ✅ (n/a)
No ViewModel, Repository, or Dao touched — purely the widget's Glance layer.

#### Complexity & Pattern Health ✅ (checked)
`StandardContent`'s name/hint row is simpler after this change (one `Row`, not a `Column` wrapping a conditional second `Text`), not more complex. The new `HintIcon` composable has 2 callers, earning its extraction (see Duplication).

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` (unused-import enforcement) passes. `git status` confirms exactly the intended files changed, no stray untracked files.

#### Naming Consistency ✅ (checked)
`HintIcon` follows the file's existing composable naming (`ProgressBar`, `FlashContent`, `ClaimedState`, ...). `ic_info.xml` matches the existing `ic_add.xml`/`ic_trophy.xml` drawable naming convention.

#### Hardcoded Values ✅ (checked, consistent with existing convention)
`ic_info.xml`'s placeholder `fillColor="#FFFBF0"` is overridden at runtime via `ColorFilter.tint(colors.onSurfaceVar)` — the identical pattern `ic_add.xml` already uses, not a new inconsistency.

#### Accessibility ✅ — the point of this fix
The hint's explanation moves from always-visible text to an icon's `contentDescription` — same information exposed, now decoupled from the layout height it used to cost. The icon carries no independent click action (the whole widget body already routes to the app), so the 48dp tap-target item doesn't apply.

#### Deprecated APIs ✅ (n/a)

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md`'s widget Display States section described both hint states as a "subtitle below the reward name" with slightly stale literal copy (the quoted strings had already drifted from the actual `Strings.kt` constants, unrelated to this change) — rewrote both bullets to describe the icon + content-description behavior and corrected the quoted text while in there.

#### Tests ✅ (0 new files; 1 existing file extended)
- `WidgetContentTest.kt`: both hint-existence assertions (`standardContent_mandatoryTaskUnloggedButPointsMet_showsHint`, `standardContent_allTasksDoneBelowCost_showsDisabledLogButtonAndHint`) extended with `assertHasContentDescriptionEqualTo` to verify the explanation actually reaches the icon, not just that a node with the right test tag exists.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass. Ran `assembleDebugAndroidTest` too even with no `AppModule`/`TestAppModule`/`@Inject` change, since a new drawable resource and widget composable restructuring are easy to get wrong in ways only a real compile catches.
- `DEV_PLAYBOOK.md`'s known-limitations entry for `StandardContent` updated to record this as a second, differently-triggered instance of the same underlying architectural gap (no shrink/scroll fallback), not a fix of the gap itself — a longer reward name at large accessibility font scale would still reproduce the same clipping.
- `MANUAL_TEST_PLAN.md` steps 8, 9, and 14 updated to describe the icon instead of the old hint-text-subtitle wording.
- This branch forked before `fix/reward-detail-completion-icons` (PR #65) merged to `main`. Rebased onto the merged `main` before finishing this pass (stash → reset → pop, clean auto-merge) so `docs/CLEANUP_LOG.md`'s already-trimmed history and `TESTING.md`'s already-updated counts from that PR weren't reintroduced or duplicated.
