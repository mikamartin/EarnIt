# EarnIt — Manual Test Plan

These three journeys are deliberately never automated, not just temporarily deferred. Each one crosses a system-process boundary (a separate Android Activity outside the app under test, or a Play Store-only API) that Compose UI tests cannot see or drive — automating them would mean either a brittle UiAutomator script tied to OS/OEM-specific picker UIs, or a test that can't run outside a real Play Store install. The underlying *data correctness* for each is already covered by automated tests at a lower layer; what's manual is only the system-boundary wiring. See [TESTING.md](TESTING.md) for the automated coverage and the overall test cadence.

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
7. Repeat steps 2–6 using "Import (Merge)" instead, starting from a non-empty state with one extra task added after export. Confirm the merge keeps both the imported and the locally-added task.

---

## Widget full flow

**Why manual:** the widget flow spans Glance's composition lifecycle, a `BroadcastReceiver`, and an activity chain (`WidgetTaskLogActivity`, `WidgetConfigActivity`) outside the main app process's Compose hierarchy — the same system-boundary problem as the file picker above. `WidgetFlashTest` already covers the one piece of pure logic in isolation (the flash-revert timer).

**Cadence:** Once per release candidate, and whenever widget-related code changes.

**Steps:**

1. Add the EarnIt widget to the home screen (long-press → Widgets → EarnIt). Confirm the **Empty/unconfigured** state shows "Long-press to configure."
2. Long-press the widget → configure: select a reward, edit the label to something different from the reward name. Confirm the widget shows the custom label and that the reward name inside the app is unchanged.
3. Confirm the widget shows the **Active** state: reward label, progress bar, and **+ LOG** button.
4. Tap **+ LOG**: pick a task, add a note, confirm. Confirm the in-activity success screen appears for ~1.5 s then auto-closes.
5. Confirm the widget briefly shows "✓ Logged!" (flash), then reverts to normal state. Confirm a notification appeared ("Task name / Logged! +X pts").
6. Force-stop the app (`adb shell am force-stop com.earnit.app` or via Android Settings). Reopen and confirm the flash revert still triggers correctly on the next log — the timer is self-contained in Glance, not tied to the app process.
7. Log tasks until all non-repeatable tasks for the tracked reward are done. Confirm the **+ LOG** button is hidden and the **Done ✓** state is shown.
8. Log enough tasks to reach the reward's point cost and complete all mandatory tasks. Confirm the **CLAIM** button replaces the log button. Tap it and confirm it opens the app to the reward detail screen.
9. Claim the reward. Confirm the widget shows the **Claimed/archived** state ("Earned and Claimed" subtitle) and that tapping the widget body opens the app.
10. Reconfigure the widget (long-press → configure) and select a new reward. Confirm the widget updates.
11. Switch the app colour scheme (Settings → Colour Scheme). Confirm the widget and both widget activities (`WidgetTaskLogActivity`, `WidgetConfigActivity`) reflect the new theme in both light and dark mode.

---

## In-app review trigger

**Why manual:** `InAppReviewTriggerTest` covers the ViewModel-level logic (the event fires exactly once, on the first claim, and not again after). What no automated test can verify is whether the Play Core review API actually shows the system review dialog — that requires a build installed from the Play Store; the API silently no-ops on debug/sideloaded builds, so a successful-looking manual run can still be a false negative unless checked carefully.

**Cadence:** Once, during the first Play Store closed-test build (the trigger logic itself can't regress without a code change that `InAppReviewTriggerTest` would catch).

**Steps:**
1. Install the app from the Play Store internal/closed-test track — not a local debug build.
2. Create a task and reward, log the task, claim the reward (must be the very first claim on this install).
3. Confirm the system "Rate this app" dialog appears.
4. Claim a second reward and confirm the dialog does not appear again.
