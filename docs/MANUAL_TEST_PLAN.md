# EarnIt — Manual Test Plan

These journeys are deliberately never automated, not just temporarily deferred. Each one crosses a system-process boundary (a separate Android Activity outside the app under test, a Play Store-only API, or background `WorkManager` execution) that Compose UI tests cannot see or drive — automating them would mean either a brittle UiAutomator script tied to OS/OEM-specific picker UIs, a test that can't run outside a real Play Store install, or a test that would need to wait 48+ real hours. The underlying *data correctness* for each is already covered by automated tests at a lower layer; what's manual is only the system-boundary wiring. See [TESTING.md](TESTING.md) for the automated coverage and the overall test cadence.

---

## Export / Import — full UI round-trip

**Why manual:** Export and import are launched via Android's Storage Access Framework (`ActivityResultContracts.CreateDocument` / `OpenDocument` in `DataScreen.kt`), which hands control to a separate system file-picker Activity outside the app's process. Compose UI tests operate on the semantics tree of the app under test; they have no visibility into the picker's UI, and that UI varies by Android version and OEM skin. The actual round-trip data correctness — the real risk — is already fully covered by `ExportImportTest.kt` (5 tests, real in-memory Room, including file-based variants through a temp `Uri`). What's left untested by automation is purely the screen wiring: button tap → picker → callback → status text.

**Cadence:** Once per release candidate, before each Play Store closed-test upload.

**Steps:**
1. Launch the app with at least one task, one reward, and one completed log/history entry present.
2. Settings → Data → "Export Backup". Save the file via the system picker; note the filename/location.
3. Tap "Clear All" (or uninstall and reinstall for a true fresh state).
4. Confirm the empty-state screens appear on Tasks, Prizes, and History.
5. Tap "Import (Replace)", select the file saved in step 2.
6. Confirm the success status text appears, and that all tasks, rewards, links, and history are restored exactly as before clearing.
7. Confirm **no** mascot unlock snackbar appears and **no** `!` badge appears on the Settings nav tab after the import — mascots earned before the backup should be silently restored, not treated as new unlocks.
8. Repeat steps 2–7 using "Import (Merge)" instead, starting from a non-empty state with one extra task added after export. Confirm the merge keeps both the imported and the locally-added task, and again no mascot notification fires.

---

## Widget full flow

**Why manual:** the widget flow spans Glance's composition lifecycle, a `BroadcastReceiver`, and an activity chain (`WidgetTaskLogActivity`, `WidgetConfigActivity`) outside the main app process's Compose hierarchy — the same system-boundary problem as the file picker above. `WidgetFlashTest` already covers the one piece of pure logic in isolation (the flash-revert timer). `WidgetActionButtonTest` and `WidgetContentTest` cover the widget's button-selection logic and rendered text/click-wiring directly (via `glance-testing`, no device needed) — steps below are trimmed where they'd otherwise just re-confirm that same content, and instead focus on what only a real widget host can prove: live Room/DataStore wiring through `provideGlance`, the OS-level activity chain, real notification/timer behaviour, and launcher-specific rendering.

**Cadence:** Once per release candidate, and whenever widget-related code changes.

**Steps:**

1. Add the EarnIt widget to the home screen (long-press → Widgets → EarnIt). Confirm the **Empty/unconfigured** state appears (exact copy covered by `emptyState_showsConfigurePrompt`). Also confirm the widget occupies a compact single-row footprint, not an oversized cell with dead space — launchers quantize `minWidth`/`minHeight` (in `earnit_widget_info.xml`) into grid rows/columns using their own formula, so this can vary by device/launcher and silently regress if those values change.
2. Long-press the widget → configure: select a reward, edit the label to something different from the reward name. Confirm the widget shows the custom label and that the reward name inside the app is unchanged. Also confirm the label field stops accepting input past `REWARD_NAME_MAX_CHARS` (40) characters.
3. Confirm the widget shows the **Active** state (reward label, progress bar, **+ LOG** button) driven by real Room/DataStore data through `provideGlance` — the button/text content itself is covered by `standardContent_unloggedTask_showsOnlyLogButton` and `standardContent_showsRewardNameAndCurrentPoints`; this step is checking the live data pipeline into that render, not the render logic itself.
4. Tap **+ LOG**: pick a task, add a note, confirm. Confirm the in-activity success screen appears for ~1.5 s then auto-closes.
5. Confirm the widget briefly shows "✓ Logged!" (flash), then reverts to normal state. Confirm a notification appeared ("Task name / Logged! +X pts").
6. Force-stop the app (`adb shell am force-stop com.earnit.app` or via Android Settings). Reopen and confirm the flash revert still triggers correctly on the next log — the timer is self-contained in Glance, not tied to the app process.
7. Configure the widget with a reward that has **no tasks linked** (button presence covered by `standardContent_noTasks_showsOnlyAddTaskButton`). Tap **ADD TASK** and confirm it opens the app straight to that reward's detail screen with the Add Task dialog already open (no extra tap needed) — this cross-activity intent → navigation → dialog chain is the part no unit test can reach.
8. Configure the widget with a reward that has tasks. Log enough points to meet the point cost **without** completing all mandatory (★) tasks. Spot-check the mandatory-task hint subtitle appears below the reward name on a real render — the hint-vs-no-hint branch itself is covered by `standardContent_mandatoryTaskUnloggedButPointsMet_showsHint` / `standardContent_notBlockedOnMandatoryTask_hidesHint`.
9. Log tasks until all non-repeatable tasks for the tracked reward are done. Spot-check that no button is shown (covered by `standardContent_allTasksDoneBelowCost_showsNoActionButton`) and that nothing renders oddly in the empty space on a real widget host.
10. Log enough tasks to reach the reward's point cost and complete all mandatory tasks. Confirm the **CLAIM** button replaces the log button. Tap it and confirm it opens the app to the reward detail screen.
11. Claim the reward. Confirm the widget shows the **Claimed/archived** state ("Earned and Claimed" subtitle) and that tapping the widget body opens the app.
12. Remove the widget from the home screen and add a new one. Confirm the config flow runs again and selecting a different reward produces a correctly configured widget. Note: long-pressing an existing widget only offers resizing — re-adding is the only way to change the tracked reward.
13. Switch the app colour scheme (Settings → Colour Scheme). Confirm both widget activities (`WidgetTaskLogActivity`, `WidgetConfigActivity`) reflect the new theme in both light and dark mode. Note: the existing widget on the home screen does **not** re-theme automatically — remove and re-add it to confirm a newly placed widget uses the updated scheme.
14. With a reward in the mandatory-task-hint state from step 8, drag-resize the widget down to its smallest footprint (e.g. a 3-column-wide, 2-row-tall home screen grid cell). Confirm the reward name, hint text, and full progress bar all stay visible with no clipping — this combination (narrow width + hint text + minimal height) is what caused the progress bar to be cut off on at least one device before `fix/widget-hint-overflow`.
15. On a device with a front-camera display cutout (punch-hole or notch), open both `WidgetTaskLogActivity` (tap **+ LOG**) and `WidgetConfigActivity` (long-press the widget → configure). Confirm the title/content on each starts clear of the status bar and cutout in both portrait and landscape — `targetSdk` 36 enforces edge-to-edge on every activity regardless of whether it opts in, so this is a real risk any time these screens' layouts change.
16. With no EarnIt widget currently placed, create a reward and link its first task. Confirm the widget nudge banner appears on Reward Detail. Tap "Add widget" and confirm `requestPinAppWidget()` triggers the launcher's system "add to home screen" placement flow (exact UI is launcher-specific and out of the app's control — Compose UI tests cannot drive it). After placing it, confirm `WidgetConfigActivity` runs normally and configuring it works as in steps 1–2. Separately, dismiss the banner via its close icon and confirm it does not reappear after restarting the app.

---

## Inactivity nudge notifications

**Why manual:** `NudgeDeciderTest` covers the decision logic (thresholds, guardrails, the two-nudge cap, reset-on-new-log) in isolation, and `NudgeWorkerTest` covers `NudgeWorker.doWork()` end-to-end — decision → real notification posted (title/body asserted via Robolectric's `NotificationManager` shadow) → settings persisted, including the permission-denied path — via `androidx.work:work-testing`'s `TestListenableWorkerBuilder`. What neither can verify: real `WorkManager` periodic scheduling actually invoking the worker in a live app process (the test builds `NudgeWorker` directly, bypassing the periodic scheduler entirely), the real Hilt `HiltWorkerFactory` constructing it via the generated assisted factory (the test also constructs it directly, bypassing Hilt), the real OS permission dialog, and a real tap on the system notification tray actually opening the app. `DataScreen.kt`'s dev-mode-gated "Inactivity nudge" card exists specifically to make this narrower remaining journey checkable in a couple of minutes instead of waiting 48/96 real hours.

**Cadence:** Once per release candidate, and whenever nudge-related code changes.

**Steps:**
1. Enable dev mode and **load full test data**, not just basic test data — this seeds many completion logs with several near-simultaneous "most recent" entries, which is the specific shape of data that once made the "48H" button silently no-op (fixed; see `NudgeDataTest`). Confirm at least one active reward exists.
2. Fresh install on Android 13+: confirm the notification-permission system dialog appears on first launch (not just when using the widget), and grant it.
3. Settings → Data & Backup → dev tools → tap **"48H"**. It backdates every completion log past 49h and immediately triggers a real check in one tap; the status line updates to "Checked — last log ~49h ago". Confirm a notification appears ("Still there?") and tapping it opens the app.
4. Tap **"48H"** again immediately. Confirm no duplicate/second notification appears (stage already recorded) even though the status line still reports the same idle age.
5. Tap **"96H"**. Confirm a *different* notification appears ("Your rewards are waiting") and replaces the first rather than stacking.
6. Tap **"96H"** again. Confirm silence — the two-nudge cap holds even though idle time is still far past both thresholds.
7. Log a real task from the app. Then tap **"48H"** again. Confirm the first nudge fires again — logging reset the cycle.

---

## In-app review trigger

**Why manual:** `InAppReviewTriggerTest` covers the ViewModel-level logic (the event fires exactly once, on the first claim, and not again after). What no automated test can verify is whether the Play Core review API actually shows the system review dialog — that requires a build installed from the Play Store; the API silently no-ops on debug/sideloaded builds, so a successful-looking manual run can still be a false negative unless checked carefully.

**Cadence:** Once, during the first Play Store closed-test build (the trigger logic itself can't regress without a code change that `InAppReviewTriggerTest` would catch).

**Steps:**
1. Install the app from the Play Store internal/closed-test track — not a local debug build.
2. Create a task and reward, log the task, claim the reward (must be the very first claim on this install).
3. Confirm the system "Rate this app" dialog appears.
4. Claim a second reward and confirm the dialog does not appear again.
