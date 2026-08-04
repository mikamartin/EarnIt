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
