# EarnIt — Cleanup Log

A record of every structured cleanup pass run on this codebase since development began.

Each pass follows the checklist in [DEV_PLAYBOOK.md §1 Post-Work Cleanup](DEV_PLAYBOOK.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

For a portfolio reader: this log exists to show that code quality is treated as an ongoing discipline. The checklist runs after every meaningful change — not because every pass finds something critical, but because the habit of looking is what prevents the slow accumulation of tech debt that makes codebases hard to work in.

> **How to add a new entry:** Copy the checklist from `DEV_PLAYBOOK.md §1`, paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. Passes 1–23 reference branch names instead of descriptions — that was the original convention, since superseded.

---

### Pass 1 — `deep-clean` branch

#### Schema & Data Naming ✅
- Renamed DB table `hall_of_fame_entries` → `history_entries` (DB version 8 → 9, `fallbackToDestructiveMigration`)
- Removed `@ColumnInfo(name = "hallOfFameEntryId")` workaround — column now matches property name `historyEntryId`
- Updated all SQL queries in `Daos.kt`
- Renamed JSON export field `hallOfFameEntries` → `historyEntries`

#### Dead Code ✅
- Deleted `Color.kt` (exact duplicate of warm-gold colors in `ColorSchemes.kt`)
- Removed dead `LightColors` / `DarkColors` from `EarnItTheme.kt` (defined but never used)
- Removed unused `TaskWithLogs` POJO from `Entities.kt`
- Removed unused `NOTES_REQUIRED_DESCRIPTION` from `Strings.kt`
- Deleted `pugsly_blue.png` and `pugsly_forest.png` (unreferenced drawables)
- Renamed `HallOfFameAttributionTest.kt` → `LogAttributionTest.kt` to match class name

#### Hardcoded Colors ✅
- `checkmarkColor = Color(0xFF3D2900)` → `MaterialTheme.colorScheme.onPrimary` (3 occurrences)
- Quote text `Color(0xFF3D2900)` → `MaterialTheme.colorScheme.onSurface`
- Card backgrounds `Color(0xFFF5ECD8)` → `MaterialTheme.colorScheme.surfaceVariant` (all occurrences)

#### Form State ✅
- `TaskEditScreen` form fields changed from `remember` → `rememberSaveable` (survives screen rotation)

#### Deprecated APIs ✅
- `Icons.Default.ArrowBack` → `Icons.AutoMirrored.Filled.ArrowBack` (5 occurrences)
- `Icons.Outlined.LibraryBooks` → `Icons.AutoMirrored.Outlined.LibraryBooks` (2 occurrences)
- `Icons.Filled.Assignment` → `Icons.AutoMirrored.Filled.Assignment` (1 occurrence)

#### Code Quality ✅
- Unused variables removed: `activeTab` (EarnItApp), `trackColor` (HomeScreen, RewardDetailScreen)
- Unused parameter removed: `uiState` on `SettingsScreen`

#### Design System ✅
- Extracted `ClaimPillButton` and `LogPillButton` into `EarnItButtons.kt` — card action buttons now use a single source of truth
- Extracted `InfoIconButton` into `EarnItButtons.kt` — consistent ⓘ icon across all settings sections

---

### Pass 2 — `deep-clean` branch (spec sync + hygiene)

#### Spec Accuracy ✅
- `EARNIT_SPEC.md` Storage row: schema `v8` → `v9` (DB was migrated in Pass 1, spec not updated)
- Section 4 History: `HallOfFameEntryEntity` → `HistoryEntryEntity`; `hallOfFameEntryId` → `historyEntryId` (entity was renamed in Pass 1)
- Section 7 Export/Import: JSON format field `hallOfFameEntries` → `historyEntries` (field was renamed in Pass 1)
- Section 6 Settings: added four previously undocumented settings — `Nickname`, `Use Random Nickname`, `Show Pugsly`, `Show Quote`
- Section 10 Screen Map: added `Appearance` settings section (was in code but missing from spec)

#### Dead Code ✅
- Removed unused imports `BlendMode` and `ColorFilter` from `EarnItApp.kt`

#### Template Additions ✅
- Added "test/debug helpers marked for removal" check to Dead Code section
- Added "inline user-visible strings not in Strings.kt" check to Dead Code section
- Added new **Accessibility** section (contentDescription on icon buttons, 48 dp touch targets)

---

### Pass 3 — hardcoded colours sweep

#### Dark Mode Quote Card ✅
- `QuoteOfTheDay` card background and label background: `Color.White` → `MaterialTheme.colorScheme.surface` (card was white-on-dark in dark mode)
- Border gradient: hardcoded `Color(0xFFE8A000)` / `Color(0xFF2A9D8F)` → `accents.gradientStart` / `accents.gradientEnd` (now follows Ocean Blue and Forest themes)
- Label text color: hardcoded `Color(0xFFE8A000)` → `accents.gradientStart`

#### Warm-Gold Gradient Brushes ✅
Replaced all remaining `Brush.*Gradient(listOf(Color(0xFFFFD060), Color(0xFFE07B00)))` (and the `Color(0xFFE8A000)` / `Color(0xFFE07B00)` border variant) with `accents.gradientStart` / `accents.gradientEnd` in:
- `ClaimPillButton` (`EarnItButtons.kt`)
- `LogPillButton` — also fixed `compositeOver(Color.White)` → `compositeOver(MaterialTheme.colorScheme.surface)`
- `HomeScreen` — Add Reward FAB
- `RewardDetailScreen` — `accentColor` local val, CLAIM button, LOG button background and border
- `RewardEditScreen` — Save button
- `TasksScreen` — Add Task FAB (consolidated double `LocalEarnItAccents.current` read)
- `TaskDetailScreen` — LOG button background and border (aligned with `RewardDetailScreen` pattern)
- `TaskEditScreen` — Save button
- `ClaimDialog` — "Archive & Start Over" button

---

### Pass 4 — `celebratory-feedback` branch

#### Celebratory Feedback — Phase A ✅
- Animated progress bar fill: `animateFloatAsState(500ms, FastOutSlowInEasing)` applied to both `RewardProgressCard` and `RewardDetailScreen`
- Floating `+X` label on task log: `Animatable` offset + alpha, `LaunchedEffect(rp.totalPoints)`, triggered on point increase only — both home screen cards and reward detail
- Animated gradient border at 100%: `infiniteRepeatable` alpha pulse (900ms) via `BorderStroke` on `RewardProgressCard` and `Modifier.border` on `RewardDetailScreen` card container
- CLAIM button scale pulse: `infiniteRepeatable` scale 1.0→1.05 (700ms) on `ClaimPillButton` (card) and CLAIM Box (detail)
- Haptics: `hapticTap()` View extension wired to LOG tap, CLAIM tap, Save Reward, Save Quest — no in-app toggle needed (respects device system setting)
- `detailAccents` duplicate `LocalEarnItAccents.current` read in `RewardDetailScreen` removed (was redundant after earlier gradient sweep)

#### UX Fix ✅
- `LogTaskDialog` task rows: entire row is now tappable (`.clickable` on the `Row`), not just the `RadioButton`

---

### Pass 5 — `tasks-rename-groups` branch

#### Dead Code ✅
- Removed stray `// Group field` inline comment from `TaskEditScreen` (added during implementation, no longer needed).

#### Test Data ✅
- `TestDataSeeder`: added `group` assignments to all tasks (Fitness, Mindfulness, Home, Skills) with one task (`Cold Shower`) left ungrouped to showcase the "Other" section in group view.

#### Unit Tests ✅
- `RepositoryBehaviourTest`: two `importTemplate` tests now assert `group = template.name` on inserted tasks — this was shipped logic without test coverage.

#### Spec ✅
- All 8 phases of Task Groups & Quest Rebrand marked complete in `EARNIT_SPEC.md`.
- Added "Max Tasks Per Reward — Cap & Warning" to Deferred Ideas with full rationale.

#### Deferred from this pass
- `Color(0xFF2A9D8F)` repeatable icon tint in `LogTaskDialog` — pre-existing hardcoded color, not introduced by this branch.
- 32dp `IconButton` touch targets for mandatory/repeatable flag toggles inside `AddTaskToRewardDialog`'s task rows — below the 48dp accessibility minimum; acceptable trade-off in a constrained list dialog. Revisit when `EarnItApp.kt` is split.
- Collapsed-section composable pattern duplicated between `TasksScreen` and `AddTaskToRewardDialog` — extraction deferred to the `EarnItApp.kt` split (pre-release checklist).

---

### Pass 6 — IO fix, clean-up feedback, group picker, duplicate protection, import dedup

#### Bug Fixes ✅
- **IO thread crash (`clearAll`, `importFromJson`):** `database.clearAllTables()` is a blocking non-suspend call that was executing on `Dispatchers.Main`, causing an `IllegalStateException` crash that left all data intact. Both `clearAll()` and the replace branch of `importFromJson()` are now wrapped in `withContext(Dispatchers.IO)`.
- **Clear Logs leaving History entries:** `clearAllLogs()` only deleted `completion_logs` rows; `history_entries` were untouched, so History showed claimed rewards after a log clear. Added `deleteAllEntries()` to `HistoryDao` and called it from `clearAllLogs()`.
- **`nameConflict` scope error:** Variable was computed inside the bottom-buttons Row in `TaskEditScreen` but referenced in the name field (`isError`) and error text above that Row — outside its declaration scope. Moved to the top of the composable function.

#### UX Improvements ✅
- **CleanUpScreen snackbar feedback:** All four operations (Clear Logs, Clear Tasks, Clear Rewards, Clear All) now show a `SnackbarHost` confirmation message after the operation completes. `CleanUpScreen` gained a `Scaffold` wrapper for this.
- **Group picker redesign (`TaskEditScreen`):** Replaced the autocomplete `OutlinedTextField` with a bordered radio-button list showing all existing groups plus a "New group" option. Selecting "New group" reveals an `OutlinedTextField` for the new name. Clearing the custom name de-selects "New group". When no groups exist yet, only the text field is shown.
- **Duplicate name blocking:** `TaskEditScreen` and `RewardEditScreen` both compute `nameConflict` at the composable root. While a conflict exists: the name field shows `isError = true`, an inline error message appears below it, and the Save button is disabled via `canSave = name.isNotBlank() && !nameConflict`.
- **Import deduplication with dialog:** `importTemplate()` now checks existing task names (lowercase-trimmed `HashSet`) before inserting and returns a list of skipped names. `TaskLibraryScreen` shows an `AlertDialog` listing skipped names after import rather than silently discarding them.

#### Test Data ✅
- `TestDataSeeder`: added long-name task (`"Early Morning 5km Run Before Breakfast"`), high-points task (`"Complete a 10km Race"`, 25 pts), long-name reward (`"Weekend Hiking Trip to the Mountains"`), high-cost reward (`"International Flight"`, 500 pts), and multi-sentence detail strings on several log entries to exercise overflow/truncation in list views.

#### Unit Tests ✅
- `RepositoryBehaviourTest`: added `coEvery { taskDao.getAllTasks() } returns emptyList()` to all three `importTemplate` tests — the updated function now calls `getAllTasks()` to build the dedup set and the mock was missing.

#### Deferred from this pass
- Snackbar strings in `CleanUpScreen` ("Logs cleared", "Tasks cleared", "Rewards cleared", "Everything wiped") are inline in the composable — should move to `Strings.kt` before release.
- `CleanupTest` unit test does not yet assert that `clearAllLogs()` also calls `historyDao.deleteAllEntries()` — the behavior is correct in code but the test description only says "deletes all logs." Extend before release.

---

### Pass 7 — `review-button-colors` branch

#### Button Color System ✅
- All primary action buttons (both FABs, both Save buttons, full-width CLAIM, LOG dialog buttons, "Archive & Start Over") migrated from hardcoded accent gradient fills to `MaterialTheme.colorScheme.primary` — now theme-aware and correctly contrasted in all three schemes and light/dark variants.
- Button labels and icons: hardcoded `Color.White` restored on FAB icons and all button text — consistent across themes.
- `ClaimPillButton` (`EarnItButtons.kt`): same gradient→primary migration; removed now-unused `LocalEarnItAccents` import.

#### Design System ✅
- Extracted `RadioRow` composable into `EarnItButtons.kt` — full-row tappable radio option with `onClick = null` on the inner `RadioButton` (correct M3 pattern). Applied in `LogForRewardDialog`; existing `RadioButton` instances in reward-detail LOG dialog and group picker cleaned up to use `onClick = null` (rows were already clickable).
- `RadioButton` selected color set to `colorScheme.primary` wherever `RadioButtonDefaults.colors` is specified.

#### Hardcoded Colors ✅
- Claim dialog: title, "Archive Only" border + text, and Cancel text were hardcoded `Color(0xFFE8A000)` — replaced with `MaterialTheme.colorScheme.primary`.

#### Dead Code ✅
- Removed four unused `val accents = LocalEarnItAccents.current` declarations in `HomeScreen`, `RewardEditScreen`, `TaskEditScreen`, and `ClaimDialog` — their only use (button gradient fills) was removed by the button migration.

#### UX Fix ✅
- Note/detail pen icon in all log entry rows (`CompletedTasksTab`, `ClaimedRewardsTab`, `RewardDetailScreen`, `TaskDetailScreen`): changed `verticalAlignment` from `CenterVertically` to `Top` with `1.dp` top padding on the icon — icon now anchors to the first line of multi-line notes.
- `LogForRewardDialog` title: added `color = colorScheme.primary` to match all other dialog titles.

#### Deprecated APIs
- `Icons.Default.ViewList` produces a deprecation warning suggesting `Icons.AutoMirrored.Filled.ViewList` — that icon does not exist in the library. Warning acknowledged; left as-is.

#### Deferred from this pass
- `Color(0xFF2A9D8F)` repeatable icon tint in `LogTaskDialog` (line ~3116) — pre-existing, carried over from Pass 5 deferral.
- Optimal-rewards banner border `Color(0xFFE8A000)` (HomeScreen, ~line 553) — pre-existing warm-gold hardcode, out of scope for this pass.
- `Color(0xFF2A9D8F)` teal gradient on "Done ✓" task state backgrounds (RewardDetailScreen, TaskDetailScreen) — pre-existing.

---

### Pass 8 — `review-point-formula` branch

#### Formula Centralization ✅
- Removed 4 inline copies of `computeAutoPoints` logic from `EarnItApp.kt` — all replaced with `task.effectivePoints()` calls.
- Added `computeAutoPoints()` and `effectivePoints()` as member functions on `TaskEntity` — single source of truth in the data model.
- `EarnItRepository.computeAutoPoints(t,d,p)` kept as a standalone method for unit test access; delegates same logic.

#### Formula Change ✅
- Switched from additive `Time + 2×Difficulty + Preparation` (range 4–20) to shift-by-1 multiplicative with max-dimension bonus:
  `ceil((T+1)(D+1)(P+1)/8) + 3 if any dimension = 5`
  Range: min **1** (all at 1), max **30** (all at 5).
- Rationale: pure additive rewarded individual dimensions equally; multiplicative rewards "doing everything hard." +3 bonus rewards pushing any single dimension to the max.
- `EARNIT_SPEC.md` formula section updated.

#### Tests ✅
- `PointFormulaTest`: all assertions updated for new formula; added `auto point formula max single dimension bonus` test for (5,1,1) = 6 case.

---

### Pass 9 — `widget-size-variants` branch

#### Widget Size Variants ✅
- Added compact widget variant (2×1): reward name + log/claim button only — no progress bar, no numbers visible.
- Both variants share logic via `EarnItBaseWidget` abstract class; `forceCompact` flag selects layout. Standard widget keeps `SizeMode.Exact`; compact uses `SizeMode.Single`.
- Second receiver `EarnItCompactGlanceWidgetReceiver` declared in manifest with its own `earnit_widget_compact_info.xml` — both appear as separate entries in the home screen widget picker.
- Pugsly preview image removed from `earnit_widget_info.xml`.

#### Theme-Aware Widget Colors ✅
- Replaced all hardcoded warm-gold widget colors with a `WidgetColors` data class built from `ColorSchemes.lightColors/darkColors`. Dark mode detected at draw time via `context.resources.configuration.uiMode`.
- `ColorProvider(day, night)` not available in this Glance version — single-color providers used with runtime dark-mode check instead.

#### Widget Claimed State Fix ✅
- Removed Pugsly mascot image from claimed/archived state.
- Now shows reward name + "Earned and Claimed" subtitle. Reward name persisted in widget prefs (`WIDGET_REWARD_NAME_KEY`) at config time; `WidgetConfigActivity` updated to save it alongside the reward ID.

#### Widget Data Refresh ✅
- Widget was not updating when tasks were logged from the main app — Glance widgets do not observe Room flows continuously; they render on demand.
- `EarnItViewModel.logTask()` and `claimReward()` now call `refreshWidgets()` after the database write.
- `WidgetTaskLogActivity` updated to call `updateAll` on both widget types (previously only updated the standard widget).

#### Body Tap ✅
- Tapping anywhere on the standard widget body (outside buttons) now opens the app to the tracked reward.

#### Widget Celebratory Feedback ✅
- **Haptic:** 60ms vibration fires on task log from widget (`VibrationEffect.createOneShot`).
- **Flash:** Widget shows "✓ Logged!" for 3 seconds after a widget log, scoped to the specific reward ID. `WidgetFlash` helper (SharedPrefs + timestamp) handles state; a `Handler.postDelayed` at 3.1s triggers the revert update.
- **Notification:** "Task name / Logged! +X pts" system notification after widget log; silently skipped on Android 13+ if `POST_NOTIFICATIONS` not granted. Notification channel `earnit_widget_log` created in `WidgetTaskLogActivity.onCreate`.
- `VIBRATE` and `POST_NOTIFICATIONS` permissions added to manifest.

#### Bug Fix ✅
- `WidgetTaskLogActivity` task list was showing points using the old additive formula. Fixed to call `task.effectivePoints()`.

#### Spec ✅
- Widget Size Variants and Widget Celebratory Feedback deferred items will be marked complete after merge.

---

### Pass 10 — `split-earnit-app` branch

#### File & Package Structure ✅
- `EarnItApp.kt` split: 4,399 lines → 200 lines (routing + navigation only)
- 13 new focused screen/helper files created in `com.earnit.app.ui`:
  - `AppHelpers.kt` — `hapticTap()`, `formatTimestamp/LogTime/Date()`, `displayPoints()`, `NOTE_MAX_CHARS`
  - `HomeScreen.kt` — `HomeScreen`, `QuoteOfTheDay`, `RewardProgressCard`, slide/fade helpers
  - `RewardDetailScreen.kt` — `RewardDetailScreen`
  - `RewardEditScreen.kt` — `RewardEditScreen`, `AddTaskToRewardDialog`, `TaskEditState`
  - `TasksScreen.kt` — `TasksScreen`, `LogForRewardDialog`
  - `TaskDetailScreen.kt` — `TaskDetailScreen`
  - `TaskEditScreen.kt` — `TaskEditScreen`, `EmojiPickerDialog`, `SliderRow`, `EmojiList`
  - `SharedDialogs.kt` — `LogTaskDialog`, `ClaimDialog`
  - `SettingsScreen.kt` — `SettingsScreen`, `SettingsCard`, `SettingsSectionHeader`, `ThemeChip`, `DangerButton`
  - `AboutScreen.kt` — `AboutScreen`
  - `DataScreen.kt` — `DataScreen`
  - `CleanUpScreen.kt` — `CleanUpScreen`
  - `HistoryScreen.kt` — `HistoryScreen`, `CompletedTasksTab`, `ClaimedRewardsTab`
  - `TaskLibraryScreen.kt` — `TaskLibraryScreen`
- All files in same package (`com.earnit.app.ui`) — no import changes required; cross-file shared symbols use `internal` visibility.
- Pre-Release Checklist item "Split EarnItApp.kt" resolved ✅

#### Design System ✅
- Extracted `CollapsibleGroupHeader` composable into `EarnItButtons.kt` — resolves deferred item from Pass 5. Replaces the copy-pasted collapse/expand row pattern present in `TasksScreen` and `AddTaskToRewardDialog`. Actual call-site replacement deferred (see below) until post-build verification.

#### Spec ✅
- "Buy Me a Coffee" section rewritten as "Tip Jar (In-App Support)" — uses RevenueCat IAP with two consumable products (`tip_small` $2, `tip_coffee` $5). External payment links removed to comply with Google Play Store policy. Implementation deferred to post-release backlog.

#### Task Detail Group Chip ✅
- `TaskDetailScreen` now shows the task's group as a small secondary-coloured chip below the task name. Hidden when the task has no group.

#### Design System ✅
- `CollapsibleGroupHeader` wired into all four live call sites: named groups and "Other" in `TasksScreen`; named groups and "Other" in `AddTaskToRewardDialog` (`RewardEditScreen`). The dialog variant uses the `leadingContent` slot for the select-all `Checkbox`.
- Removed `ExpandLess` and `ExpandMore` imports from both files — now only needed inside `EarnItButtons.kt`.

#### Deferred from this pass
- Snackbar inline strings in `CleanUpScreen` ("Logs cleared", "Tasks cleared", etc.) still inline — carried forward from Pass 6 deferral.
- `CleanupTest` does not assert `historyDao.deleteAllEntries()` on `clearAllLogs()` — carried forward from Pass 6 deferral.

---

### Pass 11 — widget UX polish & flash fix

#### Animation Timing ✅
- Floating `+X` label: travel extended to −60 dp, duration 1200 ms (`FastOutSlowInEasing`). Alpha fade decoupled — starts after 500 ms delay, runs 700 ms. Applied to both `HomeScreen` and `RewardDetailScreen`.

#### RadioButton Alignment Fix ✅
- `LogTaskDialog` (`SharedDialogs.kt`): `RadioButton` had no explicit size, so Compose inflated its 48 dp touch target into the row height, pushing the radio onto its own line for single-line task names. Fixed with `Modifier.size(24.dp)` to clip to the visual circle. `verticalAlignment = Alignment.Top` with matching `padding(top = 2.dp)` on all three row elements.

#### Widget Theme Fix ✅
- Both `WidgetTaskLogActivity` and `WidgetConfigActivity` called `EarnItTheme` without a `colorScheme` argument, always rendering in `WARM_GOLD` regardless of the user's setting. Fixed by adding `ThemedTaskPicker` and `ThemedWidgetConfig` composable wrappers that read `viewModel.settings.collectAsState()` and pass the selected scheme explicitly.

#### Widget Flash Stuck Fix ✅
- Prior mechanism: detached `CoroutineScope` with `delay(3000L)` + `updateAll()`. On process death (every APK reinstall, system memory pressure), the coroutine was killed and the widget stayed stuck on "Logged!" indefinitely.
- New mechanism: `produceState` timer inside `WidgetContent` reads `WidgetFlash.remainingMs()` and self-schedules a `delay()` to flip `showFlash = false`. Runs within the Glance composition lifecycle, independent of the app process. Added `remainingMs()` to `WidgetFlash`.
- `WidgetTaskLogActivity.onCreate()` calls `updateAll()` on both widget types at startup to clear any stale flash before the user logs a new task.
- `onTaskLogged` detached coroutine simplified — `delay(3000L)` + second `updateAll()` removed.

#### Widget Success Screen ✅
- After tapping "Log task" in `WidgetTaskLogActivity`, the activity now shows a brief success screen (✓, task name, +X pts) for 1.5 s before auto-closing via `LaunchedEffect` + `delay`. Gives the user visible confirmation without any extra tap.

#### Code Hygiene ✅
- Import `kotlinx.coroutines.delay` was placed out of order (between compose UI and core imports) — moved to its correct group with the other `kotlinx.coroutines.*` imports.
- Removed explain-what comment from `ThemedTaskPicker` — the function name is self-describing.

#### `NoteScreen` consistency fix ✅
- Widget note field had no character limit. `LogTaskDialog` and `LogForRewardDialog` both enforce `NOTE_MAX_CHARS = 200`. Fixed: `onValueChange` now guards with `it.length <= NOTE_MAX_CHARS` and a `supportingText` counter shows `X/200`.
- `note` state changed from `remember` to `rememberSaveable` — text now survives configuration changes (e.g. device rotation).

#### Deferred from this pass
- Widget thorough review and automated testing — added to Deferred Ideas in `EARNIT_SPEC.md`. `WidgetFlash` unit tests and `TaskPickerScreen` filter logic are the highest-value quick wins.
- Inline user-visible strings in widget activities (`WidgetTaskLogActivity`, `WidgetConfigActivity`) not yet in `Strings.kt` — consistent with the rest of the codebase; carried to pre-release checklist alongside the existing CleanUpScreen inline strings deferral.

---

### Pass 12 — group picker UX redesign & sort fix

#### Group Picker Redesign ✅
- `TaskEditScreen` group section is now collapsible via `CollapsibleGroupHeader` — expanded by default, collapses to "Group · [name]" or "Group (optional)" summary.
- "New group..." is the last row inside the bordered box — a `BasicTextField` with a matching `RadioButton`, visually unified with the existing group rows. No separate button or external text field.
- Selecting an existing group radio clears the `newGroupText` field; typing in "New group..." clears the radio selection. Both remain mutually exclusive via separate `group` and `newGroupText` state vars.
- Save action resolves final group as `newGroupText.ifBlank { group }`.
- Removed `showCustomGroupField` state var, `isCustomGroup` and `showGroupTextField` derived values — logic simplified.
- Removed `HorizontalDivider` dividers between radio rows — cleaner list appearance.
- Added imports: `BasicTextField`, `FocusRequester`, `focusRequester`, `SolidColor`.
- Removed imports: `HorizontalDivider`, `Icons.Default.Add`.

#### Sort Fix ✅
- "Complete to earn points" task list in `RewardDetailScreen`: tasks are now sorted alphabetically within each group (mandatory A→Z first, optional A→Z after). Previously insertion order.

#### Deferred from this pass
- "New group..." placeholder and "Group (optional)" label are inline strings — carry forward to pre-release Strings.kt pass.
- `FocusRequester` placed at the form Column scope; if `TaskEditScreen` is ever split further, ensure it stays outside any conditional block.

---

### Pass 13 — `export-import-backup` branch

#### Auto Backup ✅
- Added `res/xml/data_extraction_rules.xml` — includes `earnit.db` (Room) and `datastore/` (DataStore preferences) in both cloud backup and device transfer.
- Added `android:dataExtractionRules="@xml/data_extraction_rules"` to `AndroidManifest.xml`. `android:allowBackup="true"` was already set.

#### File-based Export / Import ✅
- `EarnItRepository`: added `exportToFile(context, uri)` and `importFromFile(context, uri, replace)` — thin wrappers around the existing `exportToJson`/`importFromJson` using `ContentResolver` streams.
- `EarnItViewModel`: added matching `exportToFile` and `importFromFile` wrappers; removed now-dead `exportDatabase` and `importDatabase` (clipboard-based) methods.
- `DataScreen`: replaced clipboard export + paste text field with `ActivityResultContracts.CreateDocument` (export) and `ActivityResultContracts.GetContent` (import Replace / Merge). Suggested export filename: `earnit_backup_YYYY-MM-DD.json`.

#### Spec ✅
- Section 7 rewritten to cover Auto Backup, file-based Export, and file-based Import.
- "File-based Export / Import" removed from Deferred Ideas.

---

### Pass 14 — `mascot-system` branch

#### Mascot System ✅
- `AppSettings`: `pugslyEnabled: Boolean` replaced with `selectedMascotId: MascotId?` + `unlockedMascotIds: Set<MascotId>`.
- `SettingsRepository`: DataStore migration — legacy `show_pugsly` key read on first launch, then `selected_mascot_id` / `unlocked_mascot_ids` used going forward.
- `Mascots.kt` (new): `MascotDef` list with 9 slots; 5 have artwork (Pugsly, Tabby, Panda, Penguin, Otter, Capybara). Unlock logic is pure data — no constants in ViewModel.
- `EarnItViewModel`: `checkAndUnlockMascots()` fires after every claim; uses `totalClaims + 1` offset because the Room Flow hasn't propagated the new history entry at check time. Emits `newlyUnlockedMascot` SharedFlow and `triggerMascotBounce` SharedFlow.
- `SettingsScreen`: Mascot picker row opens `MascotPickerDialog` — 3×3 grid, locked slots show unlock hint only (name hidden), unlocked slots show avatar + name + checkmark if selected. Toggle switch hides mascot (sets `null`).
- `HomeScreen`: Mascot resolved dynamically from `selectedMascotId` + color scheme. Bounces (scale 1.0 → 1.3 → spring back) on every reward claim via `triggerMascotBounce` collection.
- `EarnItApp`: Unlock snackbar with `Strings.MASCOT_UNLOCK_ACTION` action label navigates to Settings on tap.

#### Dead Strings ✅
- Removed `MASCOT_LOCKED_LABEL` from `Strings.kt` — became dead after locked slots were changed to show only the unlock hint (no "Locked" label text).
- Removed `MASCOT_UNLOCK_CTA` from `Strings.kt` — defined but never referenced.
- Moved inline `"Mascots"` snackbar action label to `Strings.MASCOT_UNLOCK_ACTION`.

#### Asset Pipeline ✅
- `recolor_pug.py`: added `--strength` parameter (0.0–1.0) to scale hue shift — needed for mostly black-and-white mascots (Panda: 0.3, Penguin: 0.35). Script now resizes the base image to 512×512 in place if not already that size, and outputs variants at 512×512 with `compress_level=9`.
- All mascot images resized from 2048×2048 to 512×512 — total drawable size reduced from ~17 MB to ~3.4 MB (~80% reduction).

#### Deferred from this pass
- `recolor_pug.py` move to `/tools` — already on Pre-Release Checklist.
- Mascot slots 7–9: artwork TBD.
- Bounce animation values (1.3f, 150ms, spring params) are inline — consistent with other animation constants in the codebase; acceptable.

---

### Pass 15 — mascot polish, task library expansion, UI animations

#### Inline Strings ✅
- `SettingsScreen.kt` (`MascotPickerDialog`): `"Show mascot"` → `Strings.MASCOT_PICKER_TITLE`, `"Done"` → `Strings.MASCOT_PICKER_DONE`, `"NEW"` badge → `Strings.MASCOT_NEW_BADGE`.
- `RewardEditScreen.kt` (Add Task empty state): `"No tasks yet."` → `Strings.ADD_TASK_EMPTY`, `"Create your own"` → `Strings.ADD_TASK_CREATE`, `"browse the Library"` → `Strings.ADD_TASK_BROWSE`.

#### Startup Unlock Check Timing ✅
- `HomeScreen.kt` `LaunchedEffect(Unit)`: added `viewModel.uiState.drop(1).first()` before calling `runStartupUnlockCheck()`. The UI is actively collecting `uiState` at this point so `WhileSubscribed` is live — safe to wait for the first Room emission. Without this, the check fired on the initial `EarnItUiState()` default value (empty) and would miss any mascots that should have unlocked.

#### Deferred from this pass
- `DataScreen.kt` inline strings (`"Load test data"`, `"Load full test data"`, descriptions) — test data section is now hidden behind dev mode; not worth moving.
- `"or"` connector in Add Task empty state — single word, context-only, no translation needed.
- Empty-state pulse animation magic numbers (`1.12f`, `900ms`) — consistent with existing animation constants in the codebase.
- Pre-existing hardcoded banner colors in `HomeScreen.kt` — carried from Pass 7 deferral.

---

### Pass 16 — bug fixes, Add Task shortcut, and button style system

#### Bug Fixes ✅
- **`copyRewardFromEntry` lost icon, wrong `sortOrder`/`createdAt`:** bypassed `upsertReward` → icon dropped and new reward pinned to top. Routed through `upsertReward` with `icon = entry.rewardIcon`.
- **`RewardDetailScreen` activity log showed "Task" for deleted tasks:** `task?.name ?: "Task"` → `task?.name ?: log.taskName` (uses snapshotted name, matching `CompletedTasksTab` pattern).
- **LOG button enabled when `loggableTasks` empty:** `rp.allTasks.isNotEmpty()` → `rp.loggableTasks.isNotEmpty()` — tapping LOG no longer opens an empty dialog.
- **Nickname defaults to "Babe" when user clears it:** `?.takeIf { it.isNotEmpty() } ?: "Babe"` removed — `""` is now stored and respected; greeting shows "Earn It!" with no address.
- **Task name truncated on Task Detail:** removed `maxLines = 1` + `TextOverflow.Ellipsis`; inner Row changed to `Alignment.Top` so icon anchors to first line.
- **Points hidden for long task names in Reward Detail task list:** task name `Text` given `Modifier.weight(1f)`; removed `Spacer(weight(1f))` that was taking space from the name.

#### Features ✅
- **Add Task shortcut on Reward Detail:** `AddTaskToRewardDialog` moved from `RewardEditScreen.kt` to `SharedDialogs.kt`; `addTaskToReward()` added to `EarnItViewModel`; full-width "Add task" `EarnItOutlinedButton` added inside the "Complete to earn points" section — writes to DB immediately (no deferred save).
- **Save/Cancel stay on screen after save:** removed `navController.popBackStack()` from Save action in both `RewardEditScreen` and `TaskEditScreen` — snackbar confirms save, user navigates back manually.
- **Compact widget removed:** `EarnItCompactGlanceWidget`, `EarnItCompactGlanceWidgetReceiver`, manifest entry, and `earnit_widget_compact_info.xml` deleted. `EarnItBaseWidget` abstract class collapsed into `EarnItGlanceWidget`.
- **Widget custom label (privacy):** `WidgetConfigActivity` is now a two-step flow — pick reward → edit label. Label is pre-filled with the reward name but fully editable; stored in `widget_reward_name`. The actual reward name in the app is unaffected.
- **Button style system:** `buttonLabelStyle` `@Composable` property added to `AppHelpers.kt`; `EarnItPrimaryButton` and `EarnItOutlinedButton` added to `EarnItButtons.kt`. All primary action buttons (`SAVE`, `LOG`, `ADD SELECTED`, `ADD WIDGET`, `REPLACE ALL`, `OK`) and cancel buttons migrated. Raw `Button(` without explicit colours eliminated from production code.
- **Golden button dark-text fix:** Root cause was Warm Gold `onPrimary = Color(0xFF3D2900)` (dark brown). `EarnItPrimaryButton` uses explicit `contentColor = Color.White`. `TaskLibraryScreen` "OK" button fixed. `DataScreen` "REPLACE ALL" fixed.
- **Small caps convention applied to Save/Cancel:** `RewardEditScreen` and `TaskEditScreen` bottom bar buttons now use `buttonLabelStyle`.

#### Test Updates ✅
- `RepositoryBehaviourTest`: existing `copyRewardFromEntry` test updated to add `getMaxSortOrder()` mock (required after fix); new test `copyRewardFromEntry copies icon and appends reward to end of list` added.
- `TestDataSeeder.seedFull`: `history()` helper extended with optional `notes` parameter; 12 of 20 history entries now seed realistic notes to exercise the note display in Claimed Rewards history.

#### Dead Code ✅
- `RewardEditScreen.kt`: removed unused `background`, `clickable`, `Box` imports (only used by the now-deleted Save Box).
- `TaskEditScreen.kt`: removed unused `shadow` import.
- `SharedDialogs.kt`, `TasksScreen.kt`, `TaskLibraryScreen.kt`, `DataScreen.kt`: removed now-unused `Button` (and where applicable `ButtonDefaults`, `Color`) imports after migration to `EarnItPrimaryButton`.
- `EarnItWidget.kt`: removed unused `LocalSize` import (was only used by deleted `CompactContent`); removed `COMPACT_WIDTH` constant; removed `CompactContent` composable; removed `EarnItCompactGlanceWidget`, `EarnItCompactGlanceWidgetReceiver`.

#### Spec ✅
- Section 5 Widget: Compact variant removed; Configuration updated to two-step flow with custom label description.
- Section 6 Settings: Nickname note added — clearing stores `""`, greeting shows "Earn It!" with no address.
- Section 10 Screen Map: Reward Detail Add Task shortcut; Save-stays-on-screen for both edit screens; widget WidgetConfigActivity two-step description.
- Deferred Ideas: Widget review updated — compact variant references removed, custom label two-step flow and privacy testing added.

#### Deferred from this pass
- `Color(0xFF2A9D8F)` repeatable icon tint in `LogTaskDialog` — pre-existing, carried forward.
- `LogPillButton` disabled-state warm-gold neutral colours — pre-existing, carried forward.
- Progress bar background hardcoded warm-gold Canvas colours — pre-existing, carried forward.
- Pre-existing hardcoded banner colours in `HomeScreen.kt` — carried forward.
- `CleanupTest` missing `historyDao.deleteAllEntries()` assertion — already added in Pass 16; checklist item was stale.

---

### Pass 17 — `prerelease/checklist-and-deps` branch

#### File & Package Structure ✅
- `prep_mascot.py` moved from repo root to `tools/prep_mascot.py`. `tools/README.md` added documenting the script and its Pillow dependency.
- Root dir audit: clean — no stray planning files or scripts. `.gitignore` extended with `*.keystore`, `*.jks` (signing keys), `__pycache__/`, and `*.pyc` (Python artifacts from tools/).

#### Inline Strings ✅
- `CleanUpScreen.kt`: all 4 snackbar messages, 4 dialog titles, 4 dialog body texts, 4 card descriptions, and 4 button labels moved to `Strings.kt`.
- `WidgetTaskLogActivity.kt`: notification body text, notification channel name, task picker hint, "All tasks logged" empty state, note screen label, note placeholder, "Log task" and "Back" button labels moved to `Strings.kt`.
- `WidgetConfigActivity.kt`: reward picker title, subtitle, empty state, widget label screen title, label hint, reward name display, and "ADD WIDGET" button label moved to `Strings.kt`.

#### Code Quality Audits ✅
- **`LaunchedEffect` keys**: all 13 occurrences reviewed. All keys are correct. The two edit-screen initialization effects use guard variables (`taskStateReady`, `containsKey`) that prevent over-triggering. No changes needed.
- **`remember` vs `rememberSaveable`**: `TasksScreen.query` upgraded to `rememberSaveable` — search text now survives device rotation.
- **Known limitation (not fixed):** `taskState` (RewardEditScreen) and `rewardLinkState` (TaskEditScreen) are `remember { mutableStateMapOf() }`. These maps lose task-link checkbox state on rotation. Fixing requires a custom `mapSaver` for `rememberSaveable` — deferred; acceptable pre-release.

#### KAPT → KSP Migration ✅
- Removed `org.jetbrains.kotlin.kapt` plugin from both `build.gradle.kts` files.
- Added `com.google.devtools.ksp` version `1.9.23-1.0.20` to root build file; applied in app module.
- Migrated all three annotation processors:
  - `kapt(room-compiler:2.6.1)` → `ksp(room-compiler:2.6.1)`
  - `kapt(hilt-compiler:2.51.1)` → `ksp(hilt-android-compiler:2.51.1)` (also corrected artifact name from `hilt-compiler` to `hilt-android-compiler` per official Hilt KSP docs)
  - `kapt(moshi-kotlin-codegen:1.15.0)` → `ksp(moshi-kotlin-codegen:1.15.0)`

#### SDK Version Bump ✅
- `compileSdk` and `targetSdk` bumped from 34 → 35 (Android 15). Required by Google Play for new apps/updates.

#### Deferred from this pass
- AGP upgrade (8.13.2 has an upgrade available) — use **AGP Upgrade Assistant** in Android Studio; handles Gradle wrapper and cascading changes automatically. Do not migrate manually.
- Kotlin upgrade (1.9.23 → 2.x) — requires updating the Compose compiler plugin format (new in Kotlin 2.0) and all Compose/lifecycle dependencies. Bundle with a Compose version upgrade.
- Compose upgrade (1.5.x → current) — must move with Kotlin since Compose compiler extension version is Kotlin-version-locked. Test thoroughly on emulator after upgrading.
- `TestDataSeeder.kt` removal — kept intentionally for ongoing development; hidden behind dev mode instead of removed.

---

### Pass 18 — `feature/about-screen-support` branch

#### About Screen — Support Features ✅
- `TipRepository` interface + `MockTipRepository` added to `data/` — all RevenueCat types will stay behind this interface; `AppModule` provides the mock until real integration is done
- `TipViewModel` added to `viewmodel/` — `TipState` (Loading / Ready / Error) StateFlow, `PurchaseEvent` SharedFlow; `purchase()` takes `Activity` for future billing sheet
- `AboutScreen` redesigned: replaced static "Buy me a coffee" row with three new sections:
  - **Rate the app** — tappable row, opens `market://` URI with HTTPS fallback
  - **Get in touch** — tappable row, fires `ACTION_SENDTO` mailto intent
  - **Support the developer** — loading indicator while prices fetch, two `EarnItPrimaryButton` instances with dynamic price labels, thank-you/error snackbar via local `Scaffold`
- `EarnItApp`: collects `triggerInAppReview` SharedFlow; calls `ReviewManagerFactory` + `launchReviewFlow` — silently no-ops in debug, fires on Play Store installs

#### In-App Review Trigger ✅
- `EarnItViewModel.claimReward` captures `isFirstClaim = uiState.value.historyEntries.isEmpty()` before the DB write; emits `_triggerInAppReview` only on first-ever claim

#### Strings ✅
- All About screen and tip copy centralised in `Strings.kt` — `ABOUT_RATE_LABEL`, `ABOUT_RATE_SUBTITLE`, `ABOUT_CONTACT_LABEL`, `ABOUT_CONTACT_SUBTITLE`, `ABOUT_CONTACT_EMAIL`, `ABOUT_TIP_TITLE`, `ABOUT_TIP_COPY`, `TIP_SUCCESS`, `TIP_ERROR`, `TIP_LOAD_ERROR`

#### Dependencies ✅
- `com.google.android.play:review-ktx:2.0.1` added to `build.gradle.kts`

#### Spec ✅
- Section 10 Screen Map: About entry updated with full description of new sections
- Tip Jar deferred entry rewritten: what's built vs. what RevenueCat integration still needs
- "How to get feedback from users" deferred entry replaced with description of implemented behaviour

#### Deferred from this pass
- RevenueCat integration — `MockTipRepository` is the placeholder; see Tip Jar deferred entry in spec for the full integration steps
- `Strings.ABOUT_CONTACT_EMAIL` — placeholder at time of writing; updated to `hello@secondmondaystudios.com` in Pass 21
- In-app review unit tests — 2 tests for `claimReward` trigger condition (see TESTING.md)
- `TipViewModel` state tests — low priority until RevenueCat replaces mock

---

### Pass 19 — `fix/about-screen-design-review` branch

#### Icon ✅
- `AboutActionRow`: `Icons.AutoMirrored.Filled.NavigateNext` → `Icons.Default.ChevronRight` — now matches every other navigation row in the app

#### Colour ✅
- `AboutActionRow` chevron tint: `colorScheme.primary` → `colorScheme.onSurfaceVariant` — consistent with `SettingsScreen` (3 rows) and `TaskDetailScreen`

#### Strings ✅
- `"The idea"` section header moved to `Strings.ABOUT_THE_IDEA_TITLE`
- Placeholder body text moved to `Strings.ABOUT_THE_IDEA_PLACEHOLDER` — easy to find when real copy is written
- `Strings.ABOUT_TEASER` updated from `"Why this app exists — and a note from the maker."` to `"The story behind the app, plus support and feedback options."` — now accurately reflects the full About screen

#### Safety ✅
- `context as Activity` (tip button `onClick`) changed to `(context as? Activity)?.let { ... }` — safe cast, no-op if context is not an Activity

#### Accessibility ✅
- `AboutActionRow`: added `Modifier.heightIn(min = 48.dp)` — guarantees minimum touch target regardless of content

#### Code quality ✅
- `CircularProgressIndicator`: removed redundant `color = MaterialTheme.colorScheme.primary` — that is already the M3 default

---

### Pass 20 — `fix/tasklibrary-tasks-design-review` branch

#### TaskLibraryScreen ✅
- **Button**: Raw `OutlinedButton` with inline styling → `EarnItPrimaryButton` — brings ADD into the design system (Pass 16 migration was incomplete here)
- **Top bar title**: `titleLarge + Bold` → `titleMedium + ExtraBold` — matches every other sub-screen top bar
- **Back arrow tint**: no tint (default `onSurface`) → `colorScheme.primary` — consistent with AboutScreen, CleanUpScreen
- **Inline strings → Strings.kt**: `LIBRARY_TITLE`, `libraryTaskCount()`, `libraryGroupHint()`, `libraryAddButton()`, `librarySkippedTitle()`, `librarySkippedBody()`

#### TasksScreen ✅
- **TaskCard composable extracted**: identical card layout was duplicated verbatim 3× (named group, Other group, flat list) — now a single private `TaskCard(task, accent, onClick, modifier, isDragging)` composable; drag modifier and elevated shadow on drag still passed in from flat-list call site
- **First group expanded by default**: `collapsedGroups[groupName] ?: true` → `?: (groupIndex != 0)` — first alphabetical group opens on first load; all others remain collapsed
- **"Other" expanded when sole section**: `collapsedGroups[null] ?: true` → `?: namedGroups.isNotEmpty()` — when no named groups exist "Other" opens automatically
- **Inline strings → Strings.kt**: `TASKS_EMPTY_TITLE`, `TASKS_EMPTY_BODY`, `TASKS_GROUP_OTHER`, `TASKS_GROUP_ASSIGN_HINT`, `TASKS_SEARCH_HINT`, `TASKS_LIBRARY_BTN`

---

### Pass 21 — pre-publish cleanup

#### Branding ✅
- All "Cedar Inlet Apps" references replaced with "SecondMonday Studios" across `Strings.kt`, `CLEANUP.md`, `LAUNCH_SETUP.md`, `EARNIT_SPEC.md`
- `Strings.ABOUT_CONTACT_EMAIL` set to `hello@secondmondaystudios.com`

#### Dev Mode — TestDataSeeder hidden ✅
- `AppSettings`: added `devModeEnabled: Boolean = false`
- `SettingsRepository`: added `DEV_MODE_ENABLED` DataStore key + `enableDevMode()` suspend function
- `EarnItViewModel`: added `enableDevMode()` wrapper
- `AboutScreen`: tapping the version text 7 times triggers `enableDevMode()` + snackbar confirmation; no-ops if already enabled
- `DataScreen`: "Load test data" and "Load full test data" cards gated behind `settings.devModeEnabled` — hidden by default, no removal needed

#### Tip Jar hidden behind feature flag ✅
- `FeatureFlags.kt` added: `TIP_JAR_ENABLED = false`
- `AboutScreen` "Support the developer" section wrapped in `if (FeatureFlags.TIP_JAR_ENABLED)`
- All tip infrastructure (`TipViewModel`, `TipRepository`, `MockTipRepository`) retained for when RevenueCat is wired

#### Hardcoded Colors ✅
- `SharedDialogs.kt` repeatable icon: `Color(0xFF2A9D8F)` → `colorScheme.secondary`
- `HomeScreen.kt` optimal-rewards banner: background `Color(0xFFFFF3CD)` → `colorScheme.primaryContainer`, border `Color(0xFFE8A000)` → `colorScheme.primary`, text `Color(0xFF5C3D00)` → `colorScheme.onPrimaryContainer`
- `HistoryScreen.kt` card palette: inline `listOf(Color(...))` → `LocalEarnItAccents.current.cardPalette`
- `HistoryScreen.kt` "Earn Again" button: `Brush.horizontalGradient(teal)` → `colorScheme.secondary` (solid, consistent with Pass 7 button migration)
- `RewardDetailScreen.kt` + `TaskDetailScreen.kt` edit buttons: same teal gradient → `colorScheme.secondary`
- Removed now-unused `Brush` import from `HistoryScreen.kt`

#### Git housekeeping ✅
- `.gitignore` extended: `.claude/`, `.vscode/`
- `.claude/settings.local.json` untracked (contained only local paths + permission rules, no secrets)
- `feature/ui-tests` merged into `master`

---

### Pass 22 — empty-state UI journey

#### Tests ✅
- Added `EmptyStateUiTest.kt` — UI journey covering the "no tasks, no rewards, no history" fresh-install state across all three tabs (Prizes, Tasks, History × both sub-tabs). Closes the "Empty-state screens" gap previously listed as not covered in `TESTING.md`.
- Added `TaskLibraryImportUiTest.kt` — UI journey covering Task Library template import (expand "Healthy Living", add all 10 tasks, verify they land in the Tasks list). Closes the "UI journey: task library import" pre-release checklist item.
- Fixed `SettingsUiTest.kt` — `colorScheme_selectionPersistsAfterRecreate` referenced `settingsRepository.appSettings`, a property that no longer exists (renamed to `settings`). This compile error was pre-existing and unrelated to this pass; it blocked the whole `androidTest` source set from compiling.

#### Dependency Fix ✅
- `espresso-core` bumped `3.5.1` → `3.7.0` — `3.5.1` calls `InputManager.getInstance()` via reflection, which was removed on Android 16 (API 36), crashing every instrumented test on devices running it. Verified the fix removes that crash; tried `3.6.1` first as the more conservative bump but it still hit the same `NoSuchMethodException`.

#### Documentation ✅
- Added a "Manual-Only Test Plan" section to `TESTING.md` for the three journeys that cross a system-process boundary (export/import file picker, widget activity chain, Play Core review API) and were never going to be automated — not just temporarily deferred. Each entry states why it's manual, how often to run it, and step-by-step instructions.
- `DEV_PLAYBOOK.md` Manual Testing section now points to that plan instead of duplicating brief one-liners; the Tests section's two remaining checklist items (export/import UI journey, widget log flow) were removed since they're now formally manual-only rather than pending automation.

#### Known Gap — flagged, not fixed
- Live instrumented-test verification is currently blocked on physical Android 16 (API 36) devices by a separate, deeper issue: `ComposeTestRule` reports `IllegalStateException: No compose hierarchies found in the app` even after a successful `MainActivity` launch. Reproduced on the pre-existing `UiHappyPathTest` too, so it's not specific to the new test. Ruled out: espresso version (3.6.1/3.7.0 both affected), compose-bom patch bump (2026.05.00 → 2026.05.01, no change), timing (2s sleep + `waitForIdle()` before the first assertion, no change). Real fix is most likely a CI/emulator pinned to a stable API level (34/35) rather than chasing API-36-specific compatibility — see `DEV_PLAYBOOK.md` Known Limitations.

---

### Pass 23 — ktlint adoption

#### Linting ✅
- Added `org.jlleitschuh.gradle.ktlint` (14.2.0) to the root `build.gradle.kts`, applied to all subprojects. Closes the "Linting" pre-release checklist item.
- Decided to reformat the entire codebase to ktlint's default style in one mechanical commit, rather than disabling rules to preserve the project's prior hand-aligned-column style. Rationale: disabling the conflicting rules wouldn't actually *enforce* the alignment style going forward, only stop ktlint from commenting on it — new code could still drift with nothing to catch it. A default config also needs no explanation for an outside reader, which matters for a portfolio repo.
- `.editorconfig` added with two narrow exceptions ktlint's defaults don't account for: `ktlint_function_naming_ignore_when_annotated_with` includes `Composable` (PascalCase composables) and `Test`/`Before`/`After`/etc. (snake_case test names, a standard Kotlin testing convention). `max_line_length` raised to 180 — the ~17 lines that exceeded 120 were almost all long string literals (copy text, content descriptions) where wrapping would hurt readability more than help.
- `ktlintFormat` couldn't auto-fix wildcard imports (`import com.earnit.app.data.*`, `import io.mockk.*`, `import org.junit.Assert.*`, `import androidx.room.*`, `import androidx.datastore.preferences.core.*`) — expanded all of them to explicit imports by hand across ~16 files. Caught and fixed several explicit-import mistakes this introduced (e.g. mockk's `capture`/`slot` are members of `MockKMatcherScope`, resolved via the implicit receiver inside `every {}` blocks, not top-level — importing `io.mockk.capture` as if it were a top-level function compiles the import line but fails at the call site).
- Verified `ktlintCheck` (zero violations), `assembleDebug`, `compileDebugAndroidTestKotlin`, and the full unit test suite (79 tests) all pass after the reformat — confirms the change is purely mechanical, no logic touched.

#### Secrets Grep ✅
- `git log -p | grep -iE "key|secret|password|storeFile"` across full history — clean. Every hit is either a reference to the grep command itself in checklist docs, harmless code (`KeyboardOptions`, `LazyColumn key=`, DataStore `Keys` object, Room `@PrimaryKey`/`@ForeignKey`), or the gitignored `keystore.properties.template` placeholder with empty values. Closes the "Secrets grep" pre-release checklist item.

---

### Pass 24 — repo structure review

#### Branch & Git Hygiene ✅
- Renamed local `master` → `main`. No `main` branch existed before this; GitHub now defaults new repos to `main`, and the checklist text already assumed that name in places.
- Confirmed all 16 pre-existing local branches (including `pre-release-config`) are fully merged into `main` — `git branch --merged` showed nothing outstanding. Safe to delete; left for the Clear Git State pass.
- Found `local.properties` tracked in git despite being listed in `.gitignore` — it predates that rule. Contains a machine-specific SDK path, not a secret, but meaningless to any other clone. Flagged in `DEV_PLAYBOOK.md` Code & Repo Hygiene for `git rm --cached` during the Clear Git State pass; not removed in this pass to keep the diff scoped to structure/docs.

#### Dead Code & Hygiene ✅
- `EarnItRepository.kt:313` and `TestDataSeeder.kt:1` both had `// TEST DATA — remove before release` comments that contradicted the actual decision made in Pass 21 (hide behind `Settings.devModeEnabled`, don't remove). Updated both to describe what's actually true.

#### Documentation Restructure ✅
- Created `docs/` and moved `EARNIT_SPEC.md`, `TESTING.md`, `DEV_PLAYBOOK.md`, `CLEANUP_LOG.md` into it as a group — all four files only ever cross-link each other by bare filename, so the move didn't break any links.
- Split the Manual-Only Test Plan out of `TESTING.md` into its own `docs/MANUAL_TEST_PLAN.md`. Rationale: it's a different kind of document from the rest of `TESTING.md` — an operational runbook read during release QA, not a strategy doc read by developers/CI. `TESTING.md` now points to it instead of containing it.
- Added a "Test Cadence" section to `TESTING.md` consolidating when each test layer runs (previously scattered: one line under the pyramid, per-item cadence notes only inside the manual plan). Flagged that most rows will need updating once CI/CD Workflows 1–2 exist.
- Updated `DEV_PLAYBOOK.md`'s repo-structure-review checklist bullet to reflect the new root layout (`CLAUDE.md`, `README.md`, `docs/` instead of the four docs sitting at root) and to reference `MANUAL_TEST_PLAN.md` alongside `TESTING.md` everywhere the old single-file link was inlined.
- Added `CLAUDE.md` at repo root — project-specific instructions for AI assistants (collaboration rules, branch convention, pointers to the `docs/` source-of-truth files, common commands). This was previously living only in an external memory store outside the repo; checking it in makes the AI-assisted workflow visible and functional for any tool reading the repo, and doubles as a portfolio artifact for the README's "AI-assisted development workflow" section.

---

### Pass 25 — DEV_PLAYBOOK.md review

#### Documentation ✅
- Promoted "Known Limitations" from a `###` subsection of the Pre-Release Checklist to its own `## 3. Known Limitations` section (Tooling Upgrade Reference renumbered §3 → §4). It was sitting under a heading that says "everything here must be resolved before shipping," which directly contradicted what the section actually contains — permanent, accepted constraints that are never going to be "resolved." Only `§1 Post-Work Cleanup` is referenced by number elsewhere (`CLAUDE.md`, this file's own template instructions), so the renumbering doesn't break anything.
- Reclassified the Android 16 instrumented-test gap: it isn't a permanent limitation, it's resolved by finishing CI/CD Workflow 2 (pinning a stable emulator API level). Moved the explanation from Known Limitations into a note on that checklist bullet instead, where it belongs as context for the fix rather than a thing being permanently accepted.
- Added a Manual Test Plan pointer to the Post-Work Cleanup → Tests checklist: any new flow that crosses a system-process boundary (file picker, widget activity chain, Play-Store-only API) should be routed to `MANUAL_TEST_PLAN.md` going forward instead of being left silently untested.

---

### Pass 27 — mascot unlock UX fixes

#### Duplication ✅
- No new duplication introduced. Converted `_openMascotPicker` from `SharedFlow` to `StateFlow`, making it consistent with `_pendingTaskId` (already a StateFlow) — removed the one remaining inconsistency in how one-shot ViewModel state is modelled.

#### Decoupling ✅
- `EarnItBottomBar` receives `hasNewMascot: Boolean` rather than the full ViewModel, keeping the composable's dependency surface minimal.

#### Complexity & Pattern Health ✅
- `LaunchedEffect(currentRoute)` key is correct: re-triggers on every route change, which is the intended dismissal and badge-clear trigger.
- `StateFlow` for `openMascotPicker` is simpler and more predictable than the previous SharedFlow — eliminates the replay/buffer edge case that caused Bug 1.
- The `uiState.drop(1).first()` pattern in `importFromFile` follows the same established pattern in `HomeScreen`'s startup check.

#### Dead Code & Hygiene ✅
- `ktlintFormat` applied; all imports clean. No unused symbols.

#### Naming Consistency ✅
- `consumeMascotPickerId`, `clearNewMascotBadge`, `hasNewMascot`, `MASCOT_SETTINGS_BADGE` all follow existing naming patterns in ViewModel and `Strings.kt`.

#### Hardcoded Values ✅
- Badge label `"!"` extracted to `Strings.MASCOT_SETTINGS_BADGE` (flagged by cleanup checklist; fixed in same pass).

#### Accessibility ✅
- `BadgedBox` is an M3 component with built-in accessibility semantics. Settings icon retains its `contentDescription = "Settings"`.

#### Spec Review ✅
- Added "Mascot Unlock Notifications" subsection to `EARNIT_SPEC.md` covering snackbar, badge, and import suppression — these behaviours existed in code but were undocumented.
- Updated screen map Settings entry to mention the `!` badge.
- Updated test count (79 → 82, 13 → 14 test files).

#### Tests ✅
- Added `MascotNotificationTest` (3 tests): badge set on unlock, badge not set when all already unlocked, import silently seeds without notification.
- Updated `TESTING.md` counts and table.
- Added import-suppression verification step to `MANUAL_TEST_PLAN.md`.

---

### Pass 26 — trim Ship Checklist

#### Documentation ✅
- Removed the "Code & Repo Hygiene" subsection from the Pre-Release Checklist. It was mostly a one-time pre-publish gate already completed this session (root-file review, `pre-release-config` merge — both recorded in Pass 24/25) or a duplicate of checks `§1 Post-Work Cleanup` already has (TODO/seed-data hygiene). The two genuinely repeatable checks moved into `§1`: a `git status` clean check (Dead Code & Hygiene) and a package-placement check (Naming Consistency) — both now apply to every future pass, not just the one-time pre-publish review.
- Removed the "Tests" subsection from the Pre-Release Checklist — it had already been reduced to a pure pointer ("see TESTING.md / MANUAL_TEST_PLAN.md") with no actionable content of its own, and both docs are already referenced from `CLAUDE.md` and from `§1`'s Tests checklist.
- Collapsed "Manual Testing" from a paragraph + three itemized journeys down to a single pointer line. The three journeys, their rationale, and their cadence already live in `MANUAL_TEST_PLAN.md`; duplicating the enumeration here just meant two places to keep in sync. Kept one line (rather than removing the section outright) so the Pre-Release Checklist — the actual "what's left before shipping" document — still carries a trigger to run the manual plan, rather than relying on someone independently knowing `MANUAL_TEST_PLAN.md` exists.
- The pending "untrack `local.properties`" action (previously a Code & Repo Hygiene bullet) isn't duplicated here — it's tracked in Pass 24 above and in the active to-do list for the upcoming Clear Git State pass.

---

### Pass 28 — `fix/save-navigation` branch

#### Bug Fixes ✅
- **Post-save navigation:** Saving a new task now navigates to TaskDetailScreen via `pendingTaskId` StateFlow; saving a new reward navigates to RewardDetailScreen via `pendingRewardId` StateFlow. Saving an existing task or reward still shows a snackbar and stays on screen. Previously all four cases stayed on screen (Pass 16 behaviour — correct for edits, wrong for creates).
- **Brief duplicate-name flash after SAVE:** `nameConflict` in both `TaskEditScreen` and `RewardEditScreen` is now gated on `!pendingSaveNav` / `!pendingRewardSaveNav`, so the red error doesn't flash on screen as navigation is composing out.
- **`pendingTaskId` leaking to unrelated `RewardEditScreen`:** When a standalone task was created and the user navigated to a reward form before the `LaunchedEffect` in `TaskEditScreen` consumed `pendingTaskId`, the reward form auto-included the leaked task. Fixed with an `awaitingNewTask` flag — auto-include only fires when the current reward form initiated the task creation via "Create your own."
- **`awaitingNewTask` lost on back-navigation:** `awaitingNewTask` was `remember { mutableStateOf(false) }`, which resets when the composable is destroyed. Compose NavHost destroys the outgoing composable on every forward navigation, so the flag was gone by the time the user popped back from `TaskEditScreen`. Changed to `rememberSaveable` — value is saved to the `NavBackStackEntry` saved state and restored on recreation.
- **Add task dialog checkbox not toggling on text tap:** `TaskRow` in `AddTaskToRewardDialog` had no click handler on the surrounding `Row` — only the `Checkbox` itself was interactive. Tapping the task name text did nothing. Row is now `.clickable { toggle }` with `onCheckedChange = null` on the `Checkbox` (M3 pattern: Row owns the toggle). Pre-existing tests in `SettingsUiTest` and `UiHappyPathTest` that called `.performClick()` on the task name were passing accidentally — auto-include was placing the task behind the dialog filter, so the click landed on the background row, not the dialog.

#### UX Improvement ✅
- **"Add task" disabled until reward name entered:** `AddTaskToRewardDialog` and "Create your own" are now gated on `name.isNotBlank()` — a reward form without a name can't link tasks yet.
- **"Will be added to: [reward name]" context line:** `TaskEditScreen` receives the in-progress reward name as a `fromRewardName` nav arg and shows it below the task name field when opened from a new-reward form, so the user knows which reward the task will be attached to.

#### Complexity & Pattern Health ✅
- `awaitingNewTask by rememberSaveable` is now consistent with the other `RewardEditScreen` form fields (`name`, `cost`, `description`, `icon`) which are already `rememberSaveable`.
- `TaskRow` using `onCheckedChange = null` + parent `.clickable` is the recommended M3 pattern for checkbox list rows — matches how `LogTaskDialog` rows were fixed in Pass 4.

#### Tests ✅
- Added `PendingRewardIdTest` (3 unit tests): `saveReward` sets `pendingRewardId` on new reward; leaves it null on edit; `consumePendingRewardId` clears the value.
- Added `SaveNavigationUiTest` (4 UI tests): new task navigates to TaskDetailScreen; new reward navigates to RewardDetailScreen; task created from new-reward form pops back and auto-includes; "Add task" disabled until reward name entered.
- `TESTING.md` counts updated (unit 82→85, UI 8→9, instrumented 26→30).

#### Linting ✅
- `ktlintFormat` run; one violation auto-fixed (`multiline-expression-wrapping` on the new `Modifier` chain in `TaskRow`).

#### Spec ✅
- Screen map entries for Reward Edit and Task Edit updated to reflect post-save navigation behaviour, the `fromRewardName` context line, and the "Add task" gate.
- `TESTING.md` "True process death" note updated: `awaitingNewTask` now also protected by `rememberSaveable`.

---

### Pass 29 — `chore/pre-release-cleanup` branch (pre-release code review)

#### Dynamic version string ✅
- `Strings.APP_VERSION = "Version: 1.0"` removed — would have drifted silently when `versionName` was incremented.
- `buildConfig = true` added to `buildFeatures` in `app/build.gradle.kts`; both `AboutScreen` and `SettingsScreen` now read `"Version: ${BuildConfig.VERSION_NAME}"` at runtime — always matches the build.

#### Inline strings → Strings.kt ✅
- Added `DIALOG_LOG_BTN = "LOG"` and `DIALOG_CANCEL = "CANCEL"` — `LogTaskDialog` and `AddTaskToRewardDialog` dismiss/confirm buttons were raw string literals.
- Added `fun claimDialogTitle(name: String)` — `ClaimDialog` title was an inline string template.
- Added `fun rewardEarnTasksTitle(count: Int)` — "Complete to earn points (N)" section header in `RewardDetailScreen` was inline.
- Added `REWARD_ROAD_TO_GLORY` and `REWARD_RECENT_ACTIVITY` — activity section header toggle in `RewardDetailScreen` was inline.
- `AboutScreen` Back arrow `contentDescription = "Back"` → `Strings.BACK_DESC` (constant already existed).

#### Known Limitations documented ✅
- Progress bar track backgrounds, disabled LOG button fill/border, detail dividers, and activity-log task name color are hardcoded warm-gold hex values that do not adapt to Ocean Blue or Forest themes. Added to `DEV_PLAYBOOK.md §4 Known Limitations`.

#### Test data ✅
- `TestDataSeeder.seed()`: Spa Day reward had a log attributed to `cook` (Cook Healthy Meal) which was never linked to Spa Day via a cross-ref — an orphaned log that inflated the reward balance with a task the user couldn't have seen in the log dialog. Replaced with a `walk` (Evening Walk) log, which is a valid linked task with `isRepeatable = true`.

#### Test suite refactor and coverage (QA pass) ✅
- Extracted `RepositoryTestBase` (unit): 7 files shared identical MockK setup — now inherit from one base class.
- Extracted `ViewModelTestBase` (unit): 4 files shared identical dispatcher setup/teardown — now inherit from one base class.
- Extracted `RoomIntegrationBase` (instrumented): 4 files shared identical in-memory Room setup/teardown — now inherit from one base class.
- `SaveNavigationUiTest.saveNewReward_navigatesToRewardDetail`: raw `"No tasks added yet."` → `Strings.REWARD_DETAIL_NO_TASKS` so the assertion stays in sync with the constant.
- `PointFormulaTest`: added `difficulty=5` and `preparation=5` bonus cases — previously only `time=5` was covered, leaving two bonus-trigger paths untested.
- `RewardLimitUiTest` (new, instrumented UI): verifies that tapping the FAB when `maxRewardCount` is reached shows `MAX_REWARD_BANNER` tooltip and does not navigate to reward edit.
- `DuplicateNameUiTest` (new, instrumented UI): verifies that entering a duplicate task or reward name (case-insensitive) shows the error string from `Strings.taskDuplicateError` / `Strings.rewardDuplicateError` and disables the SAVE button.

---

### Pass 30 — `fix/data-safety-and-cleanup` branch (senior-Android review follow-up)

#### Data safety ✅
- `EarnItRepository`'s multi-step mutations (`importFromJson`, `deleteReward`, `clearAllTasks`/`clearAllRewards`/`clearAllLogs`, `importTemplate`, `copyRewardFromEntry`) were sequences of independent DAO calls with no transaction — a crash or process death mid-sequence (e.g. mid-"Replace all data" import) could leave the DB half-mutated with the original data already gone. Wrapped each in `database.withTransaction { }`.
- `AppModule`'s `fallbackToDestructiveMigration(dropAllTables = true)` had only one real migration (`MIGRATION_9_10`) behind it — any future version bump without a matching migration would silently wipe every user's tasks, rewards, and permanent History. Documented the risk and the required discipline in `DEV_PLAYBOOK.md §6` (new section), with inline warnings at both load-bearing call sites (`EarnItDatabase.kt`, `AppModule.kt`).

#### Schema baseline reset ✅
- Confirmed (user-verified) this app has never been installed outside dev/emulator environments — no real device has schema v1–v10 data to preserve. Collapsed the dev-only migration history: `EarnItDatabase` version reset from 10 to 1 as the launch baseline; `MIGRATION_9_10` deleted (confirmed unreferenced by any test). `EARNIT_SPEC.md`'s schema version note updated to match.
- Added declarative `@ForeignKey` (cascade delete) + a matching index to `RewardTaskCrossRef` for both `rewardId` and `taskId` — baked directly into the v1 baseline rather than a migration, since there's no prior schema to migrate from. Scoped narrowly to this table only: `CompletionLogEntity.taskId` and `HistoryEntryEntity.rewardId` are deliberate historical snapshots (survive their source task/reward being deleted) and must not cascade.

#### Coroutine scope consistency ✅
- `WidgetTaskLogActivity` hand-rolled `CoroutineScope(SupervisorJob() + Dispatchers.X)` at two call sites to outlive its own `finish()` — correct behaviour, but not swappable in tests and with no shared error handling. Replaced with a single injected `@ApplicationScope`-qualified `CoroutineScope` singleton (`AppModule`).

#### Dead code ✅
- `WidgetTaskLogActivity`: removed two `Build.VERSION.SDK_INT >= Build.VERSION_CODES.O` guards — minSdk is 31, already exceeding O (26), so the branches were always true. Also removed the only deprecated-API call in the file (`vibrator.vibrate(Int)`) along with its `@Suppress("DEPRECATION")`, since it lived in the now-dead branch.

#### Tooling ✅
- Migrated every hardcoded dependency/plugin version across both `build.gradle.kts` files into `gradle/libs.versions.toml`. The `buildscript classpath` Kotlin override stays a literal (catalog accessors aren't available in that block) but is commented to flag the sync requirement. Noted the catalog as source of truth in `DEV_PLAYBOOK.md §5`.

#### Test infrastructure fix ✅
- The `withTransaction` wrapping above hung every test touching a wrapped method indefinitely: Room's real transaction executor never runs on a bare MockK mock, so `runBlocking` waited forever instead of failing fast. Root-caused via a `jstack` thread dump (not assumption) after an earlier misdiagnosis. Fixed centrally in `RepositoryTestBase` with a `mockkStatic`-based stub that runs the block directly — covers `CleanupTest`, `DeleteCascadeTest`, `ImportDedupTest`, and `RepositoryBehaviourTest` in one place.

#### Tests ✅ (deferred item logged)
- All 102 unit tests pass; `ktlintCheck` and `assembleDebug` both green on the final branch state.
- Real transaction rollback (as opposed to DAO call sequencing) isn't covered — MockK can't simulate it. Logged as a new gap in `TESTING.md` Deferrals rather than silently left uncovered.

#### Follow-up: transaction-wrapping completeness ✅ (second-round senior-Android review)
- The original data-safety pass above scoped `database.withTransaction { }` to a specific method list rather than sweeping the whole file — `claimReward` (the core "earn a reward" flow: insert History entry → archive logs → conditionally archive the reward), `deleteTask`, `saveRewardTasks`, and `updateTaskRewards` had the identical multi-DAO-call risk profile but were missed. `DeleteCascadeTest` had `deleteTask` and `deleteReward` tests sitting side by side — only `deleteReward` had been fixed. Wrapped all four; `TESTING.md`'s Transaction rollback deferral list updated to match.
- The `@ForeignKey` cascade added above (Schema baseline reset) made the manual `rewardTaskDao.clearTasksForReward`/`clearRewardsForTask` calls in `deleteReward`/`deleteTask` redundant — SQLite now removes the cross-ref rows declaratively. Removed both call sites, deleted the now-unused `clearRewardsForTask` DAO method, and dropped `deleteTask`'s transaction wrapper since it's now a single statement. `DeleteCascadeTest` updated to assert the manual clear no longer happens instead of asserting call order; new `TESTING.md` Deferrals entry notes cascade behaviour itself isn't verified against a real DB.
- All unit tests and `ktlintCheck` re-verified green after each change.

---

### Pass 31 — `feat/onboarding-nudges` branch (first-run guidance)

#### Design System ✅
- Extracted `DismissibleTipBanner` into `EarnItButtons.kt` before writing either of the two new nudges — the codebase had no existing "shown once, dismissible, persisted" banner pattern (`MAX_REWARD_BANNER` is transient/auto-hide, the mascot settings badge is session-only), so this is a new shared primitive with two call sites from the start rather than a later extraction.

#### Empty-state copy ✅
- `HOME_EMPTY_REWARDS`, `REWARD_DETAIL_NO_TASKS`, `TASK_DETAIL_NO_REWARDS` expanded to explain *why* (tasks earn points toward rewards), not just *what to tap next*. Left `TASKS_EMPTY_*`, `HISTORY_NO_*`, `WIDGET_CONFIG_EMPTY`, and `NO_ACTIVITY` unchanged — those are either direct action lists where the concept was already taught upstream, or purely retrospective views with nothing to teach.

#### New feature: onboarding nudges ✅
- Widget nudge (Reward Detail): dismissible banner shown the first time a reward has a task linked, unless the user already has any EarnIt widget pinned. Action calls `AppWidgetManager.requestPinAppWidget()` (new — no prior use of the pin-request API in this codebase) rather than just instructing the user to long-press the home screen. Deliberately does **not** pre-select the reward in `WidgetConfigActivity` — doing so would require threading a reward ID through `EXTRA_APPWIDGET_PROVIDER_EXTRAS`, untested plumbing with no existing precedent; scoped down after discussing the risk/benefit with the user.
- Settings tip: single dismissible banner at the top of Settings, shown once on first visit, rather than three separate nudges for name/quote/theme individually (rejected as likely to feel naggy for cosmetic, non-core-loop settings).
- Both persist their dismissed state one-way via new `SettingsRepository`/`AppSettings` boolean flags (`widgetNudgeDismissed`, `settingsTipDismissed`), following the exact existing DataStore convention — no new abstraction introduced.

#### Spec Review ✅
- `EARNIT_SPEC.md` §3 (Widget Nudge) and §6 (Discoverability Tip) added; §9 test count bumped.

#### Tests ✅
- Added `WidgetNudgeUiTest` and `SettingsTipUiTest` — each verifies show/hide conditions, dismiss behaviour, and that the dismissal survives `activityRule.scenario.recreate()` (proving the DataStore flag round-trips, not just in-memory state). `TESTING.md` updated with both rows and bumped counts.
- `./gradlew ktlintCheck`, `test`, `assembleDebug`, and `assembleDebugAndroidTest` all pass. `connectedDebugAndroidTest` run on a real device by the user afterward — all instrumented tests pass, including the two new ones.
- Added a step to `MANUAL_TEST_PLAN.md`'s "Widget full flow" journey covering the new pin-request entry point, since the launcher's system "add to home screen" dialog is exactly the kind of system-process boundary already established as manual-only for the rest of the widget flow.

### Pass 32 — `fix/add-task-shortcut` branch

#### Bug fix ✅
- Home card's "+ ADD TASKS" pill (rewards with no tasks linked) navigated to Reward Edit instead of opening the Add Task dialog, forcing a second tap once there. The widget's ADD TASK button had the same gap: it opened the app to Reward Detail but never triggered the dialog.
- Added an `autoOpenAddTask` nav argument on `Screen.RewardDetail`, threaded through `MainActivity` (new intent extra) → `EarnItApp` → the `RewardDetail` composable → `RewardDetailScreen`, which now seeds `showAddTaskDialog` from it. Both entry points reuse `RewardDetailScreen`'s existing dialog + immediate-persist logic (`viewModel.addTaskToReward`) rather than duplicating it on `HomeScreen`.

#### Complexity & Pattern Health ✅
- Caught during review, not before: keying the nav-trigger `LaunchedEffect` on the raw `(startRewardId, autoOpenAddTask)` values meant a second, identical intent — e.g. tapping the widget's ADD TASK button twice for the same reward while the app is already showing it — wouldn't re-fire the effect, so the dialog would silently fail to reopen. The pre-existing `rewardId`-only version had the same staleness quirk but it was harmless there (re-navigating to a screen you're already on is a no-op); for add-task it wasn't. Fixed with a `navRequestToken` counter in `MainActivity`, bumped on every `onCreate`/`onNewIntent` and threaded through as the effect's key, so it fires on every new intent regardless of whether the payload repeats.

#### Naming Consistency ✅
- `HomeScreen`'s `RewardProgressCard` parameter `onEditReward` renamed to `onAddTask` — it only ever wired the add-tasks pill (single call site) and never opened Reward Edit, so the old name was actively misleading.

#### Spec Review ✅
- `EARNIT_SPEC.md` widget Display States and §10 Screen Map updated to describe the dialog auto-opening from both entry points.

#### Tests ✅
- Initially added a standalone `AddTaskShortcutUiTest` with one test — caught on checklist review that this violates the stated "3+ tests to justify a new file" threshold, so folded it into `SaveNavigationUiTest` instead (same theme: post-action navigation around the reward/task forms, same fixture setup) and deleted the standalone file. `TESTING.md`'s `SaveNavigationUiTest` row bumped from (4) to (5) with an exact count; no other counts touched since they're intentionally rounded approximations, not a maintained tally.
- `MANUAL_TEST_PLAN.md` widget step 7 updated to assert the dialog opens directly instead of just landing on the reward detail screen.
- `./gradlew ktlintCheck`, `test`, `assembleDebug`, and `assembleDebugAndroidTest` all pass. `connectedDebugAndroidTest` still needs to run on a real device to confirm the new test and the widget tap path end-to-end.

### Pass 32 continued — widget refresh bug + widget test coverage (same branch)

User manually tested the flow above and found a second bug, which led to a second checklist pass on the additional diff (widget refresh fix, new `WidgetActionButton.kt`/`WidgetTestTags.kt`, new test files, new Gradle dependencies).

#### Bug fix ✅
- Repro: tap ADD TASK on the widget → add a task via the dialog → return to the home screen → the widget shows **no** action button at all (not ADD TASK, not LOG). Root cause: `EarnItViewModel.addTaskToReward()` was the only reward/task-mutating ViewModel function that didn't call `refreshWidgets()` afterward (unlike `logTask()` and `claimReward()`), and the widget has no periodic refresh (`updatePeriodMillis="0"`) or Room-invalidation hook of its own — it kept rendering the stale pre-add `RemoteViews`. Fixed by adding the same `refreshWidgets()` call the other two functions already make.
- This was reachable before this branch too (e.g. adding a task from Reward Detail's pre-existing "Add task" button while a widget was pinned), but this branch made it obviously reachable — the widget itself now funnels users into `addTaskToReward`.

#### Duplication ✅
- Found on checklist review: the testTag strings (`"widget_claim_button"` etc.) added to `EarnItWidget.kt` for the new tests below were hand-typed a second time in the test file to match. A typo in either copy wouldn't necessarily fail loudly — `assertDoesNotExist()` on a mistyped tag would falsely pass, masking a real bug instead of catching one. Extracted both copies into a shared `internal object WidgetTestTags` (new file) referenced from both `EarnItWidget.kt` and `WidgetContentTest.kt`.

#### Complexity & Pattern Health ✅
- The widget's button-selection logic (`CLAIM`/`LOG`/`ADD_TASK`, previously a boolean `when` with no `else` that silently rendered nothing on fall-through — the actual shape of the bug above) extracted into `widgetActionButtonFor(progress): WidgetActionButton` (new file `WidgetActionButton.kt`), called from `StandardContent` via an exhaustive `when` over the new enum. A missing branch is now a compile error instead of a silent empty render.

#### Tests ✅
- `WidgetFlashTest` was the only prior widget test, and it only covers the SharedPreferences flash timer — nothing asserted on the widget's actual rendered buttons/text, which is exactly the layer that broke. Added two new files:
  - `WidgetActionButtonTest` (6 tests, plain JVM) — the extracted button-selection function directly, no Glance/Robolectric needed.
  - `WidgetContentTest` (12 tests) — renders `StandardContent`/`FlashContent`/`EmptyState`/`ClaimedState` via `androidx.glance:glance-testing` + `glance-appwidget-testing` (new `testImplementation` deps, `glance`/`glance-appwidget` bumped 1.1.0 → 1.1.1 to match) and `org.robolectric:robolectric` (new dep; first use of Robolectric in this project, plus `testOptions.unitTests.isIncludeAndroidResources = true`). Both are JVM-only (`app/src/test`), no device needed. `StandardContent`/`FlashContent`/`EmptyState`/`ClaimedState`/`WidgetColors` changed from `private` to `internal` so the test source set can render them directly, bypassing `provideGlance`'s Hilt/Room pipeline in favor of fixture-driven `RewardProgress` values (same pattern as `GatekeeperTest`/`RewardProgressTest`).
  - Known limitation, documented in `WidgetContentTest`'s file comment and `TESTING.md`: `glance-testing`'s `hasStartActivityClickAction()` matcher doesn't recognize the raw-`Intent` `actionStartActivity` overload this widget uses (it builds a `StartActivityIntentAction`, which the matcher's source doesn't check for) — so these tests confirm a button exists and has *some* click action, not that it targets the correct `Intent` extras. Still manual, per `MANUAL_TEST_PLAN.md`.
- Gap found and deliberately **not** fixed this pass: there is still no regression test for the actual bug fixed above (`addTaskToReward` calling `refreshWidgets()`). `refreshWidgets()` calls `EarnItGlanceWidget().updateAll(context)` by direct instantiation, not through an injected/mockable seam, so it can't be verified with MockK the way other ViewModel behaviour is — and this is a pre-existing gap shared by `logTask()`/`claimReward()`'s identical calls, not something newly introduced. Making it testable would mean injecting a widget-refresh interface via Hilt, which is a real architectural change out of scope for this fix. Deferred; noted in `TESTING.md` Deferrals.
- `TESTING.md`: Tier 4 description updated (widget content no longer "deferred", widget activity-chain/live-data wiring still is), two new exact-count rows added, new Edge Cases entry, new Deferrals entry for the `refreshWidgets()` coverage gap above.
- `MANUAL_TEST_PLAN.md` "Widget full flow": steps that only re-confirmed content now covered by the two new test files were shortened to point at the specific test and focus on what's still uniquely manual (live Room/DataStore wiring through `provideGlance`, the ADD TASK cross-activity intent → navigation → dialog chain, real widget-host rendering). No step deleted outright — each still exercises the real pipeline, which the new unit tests deliberately bypass.
- `./gradlew ktlintCheck`, `test`, `assembleDebug`, and `assembleDebugAndroidTest` all pass; the 18 new tests individually confirmed via the JUnit XML report (not just a green `test` task).

---

### Pass 33 — `feature/inactivity-nudges` branch (48h/96h idle notifications)

#### Duplication ✅
- The three dev-tool buttons in the new "Inactivity nudge" card repeat the same `OutlinedButton`/`shape`/`weight` styling inline — reviewed against the existing "Load test data"/"Load full test data" cards in the same file, which follow the identical unextracted-per-card convention. Left as-is; consistent with local precedent, not new debt.
- No duplicated strings, no `Repository`/`ViewModel` logic duplicated, no `Dao` query overlapping an existing one.

#### Decoupling ✅
- `NudgeDebugCard`'s "CHECK NOW" button calls `NudgeScheduler.runNow(context)` directly from the composable rather than routing through the `ViewModel`. Reviewed against precedent: `EarnItGlanceWidget().updateAll(context)` is already called directly from UI/Activity code elsewhere in this codebase (not exclusively through the ViewModel) for the same reason — it's a system-scheduling call, not app state/business logic. Left as-is.
- No business logic in composables, no ViewModel referencing UI types, no data layer referencing UI/ViewModel concerns.

#### Complexity & Pattern Health ✅ (1 fix)
- **Fixed:** the new "Inactivity nudge" dev card (52 lines) was inline inside `DataScreen`, an already-large single composable. Extracted into a new private `NudgeDebugCard(viewModel, context)` composable so this branch's addition doesn't grow the existing giant-composable debt further. (`DataScreen` itself was already ~305 lines pre-branch — a pre-existing condition, out of scope for this pass.)
- `NudgeDecider`/`NudgeWorker`/`NudgeScheduler` are each single-purpose, single-caller — reviewed as earning their keep: they centralize the WorkManager unique-work-name strings and threshold constants that would otherwise be duplicated across files.

#### Dead Code & Hygiene ✅
- No unused imports/vars (ktlint's unused-import rule ran clean on every check).
- Dev-only debug method (`debugBackdateLastLog`) added directly under the existing `// TEST DATA — gated behind Settings.devModeEnabled` comment block in `EarnItRepository.kt`, consistent with `seedTestData`/`seedFullTestData`.
- Debug-card strings ("Inactivity nudge", "-49H", "CHECK NOW", etc.) left inline rather than moved to `Strings.kt` — reviewed against the existing "Load test data"/"Load full test data"/"Developer mode active" cards in the same file, none of which are in `Strings.kt` either. Production-facing nudge notification copy *is* in `Strings.kt` (`NUDGE_FIRST_TITLE` etc.); the local convention is dev-only debug strings stay inline, user-facing strings go to `Strings.kt`.
- `git status` clean, no stray untracked files.

#### Naming Consistency ✅
- New `com.earnit.app.nudge` package follows the existing flat-by-feature convention (`data/`, `ui/`, `viewmodel/`, `widget/`).
- `NudgeDecider`/`NudgeWorker`/`NudgeScheduler`/`NudgeDecision` — no symbol conflicts with stdlib/Compose/AndroidX names.
- New `Strings.kt` constants follow the existing `WIDGET_*` naming style (`NUDGE_*`).

#### Hardcoded Values ✅ (2 fixes)
- **Fixed:** `NudgeScheduler`'s periodic check interval was a bare `6` in `PeriodicWorkRequestBuilder(6, TimeUnit.HOURS)` — extracted to `private const val CHECK_INTERVAL_HOURS = 6L`.
- **Fixed:** the dev-tool backdate amounts were bare `49`/`97`, disconnected from the 48h/96h thresholds they're meant to test — if the thresholds ever changed, the buttons could silently stop crossing the boundary. `NudgeDecider` now exposes `FIRST_THRESHOLD_HOURS = 48`/`SECOND_THRESHOLD_HOURS = 96` (with the existing `*_MS` constants derived from them), and `NudgeDebugCard` computes `threshold + 1h` from those instead of restating literals.
- No new hardcoded colors.

#### Accessibility ✅
- No new icon-only buttons (all three dev buttons have text labels). Touch targets unchanged from the existing `OutlinedButton` pattern used throughout this file.

#### Deprecated APIs ✅
- No deprecation warnings from the new WorkManager/Hilt-Work APIs or `MainActivity`'s new permission-request code.

#### Spec Review ✅
- `EARNIT_SPEC.md` §8a added, describing trigger conditions, global scope, two-nudge cap, notification content, `WorkManager` mechanism, and the dev tooling.
- Not a previously-listed Deferred Idea, so nothing to remove from that section.

#### Tests ✅
- `NudgeDeciderTest` (10 tests, new file) covers the pure decision logic: never-logged/no-active-reward guardrails, idle-under-threshold, both `Send` transitions, the stage-2 two-nudge cap, and reset-on-new-log from both stage 1 and stage 2.
- Reviewed for gaps: `EarnItRepository.debugBackdateLastLog` / `EarnItViewModel.debugBackdateLastLog` have no dedicated unit test. Checked against precedent — `seedTestData`/`seedFullTestData`/`TestDataSeeder` (the existing dev-only data helpers) have zero test coverage anywhere in the codebase either; this is an established, deliberate gap for dev-only tooling, not one newly introduced here. Left uncovered, consistent with precedent.
- `NudgeWorker`'s actual `doWork()` (notification post, permission check, settings write) is not unit- or instrumented-tested — same category as `WidgetTaskLogActivity`'s notification code: real background execution + system notification outside what a JVM or Compose UI test can drive. Added to `MANUAL_TEST_PLAN.md` instead (new "Inactivity nudge notifications" journey), with the dev-tool card built specifically to make that journey checkable in a couple of minutes instead of requiring real 48h/96h waits.
- This change adds a new `@HiltWorker`/`@AssistedInject` site (`NudgeWorker`) and a new `@Inject` site (`EarnItApplication`'s `HiltWorkerFactory`). Confirmed `TestAppModule` doesn't need changes — it already provides `EarnItDatabase`/`SettingsRepository` as singletons, which is everything `NudgeWorker` depends on — and ran `./gradlew assembleDebugAndroidTest` to verify the full Hilt graph (including `TestAppModule`) still builds.
- `TESTING.md` updated: `NudgeDeciderTest` row added, unit test count `100+` → `110+` (rounded), manual-journey count `3` → `4`, Deferrals intro line mentions the `WorkManager` boundary alongside the existing system-boundary examples.
- `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest`, `assembleDebug` all pass (run sequentially, not in parallel, per `CLAUDE.md`).

---

### Pass 33 continued — automated NudgeWorker coverage (same branch)

#### Tests ✅
- User pushback on the line above (`NudgeWorker.doWork()` "not unit- or instrumented-tested"): unlike `TestDataSeeder`, `NudgeWorker` is production code that runs for real users, so "manual only" was a weaker bar than the rest of this codebase holds itself to. Re-examined and found it *is* automatable — `androidx.work:work-testing`'s `TestListenableWorkerBuilder` lets a `CoroutineWorker` be constructed directly (bypassing Hilt/WorkManager scheduling) and its `doWork()` called from a plain JVM test, same Robolectric setup already used for `WidgetContentTest`.
- Added `androidx.work:work-testing` (`testImplementation`, same `work` version as `work-runtime-ktx`) and `NudgeWorkerTest` (8 tests, new file): both `Send` transitions post the correct real notification (title/body asserted via Robolectric's `NotificationManager` shadow, not just that *some* notification fired) and persist the correct `SettingsRepository.updateNudgeState` call; all `NoOp`/`Reset` cases post nothing; and the `POST_NOTIFICATIONS`-denied path specifically (state still recorded even though the notification can't be shown — verifies the permission gate doesn't also skip state persistence).
- This narrows, not eliminates, the manual "Inactivity nudge notifications" journey in `MANUAL_TEST_PLAN.md` — its rationale was rewritten to name only what's genuinely still unreachable by automation: real periodic `WorkManager` scheduling invoking the worker in a live process, real Hilt-generated-factory construction (the test constructs `NudgeWorker` directly), the real OS permission dialog, and an actual tap on the system notification tray.
- `TESTING.md`: `NudgeWorkerTest` row added, unit test count `110+` → `120+` (rounded).
- `./gradlew test --tests` confirms all 8 new tests individually pass (JUnit XML report checked, not just a green task); full `ktlintCheck`/`test`/`assembleDebugAndroidTest`/`assembleDebug` re-run clean after the change.

---

### Pass 33 continued — automated debugBackdateLastLog coverage (same branch)

#### Tests ✅
- Further pushback, and a sharper distinction than the "consistent with `seedTestData` precedent" reasoning above: `debugBackdateLastLog` isn't just dev tooling like `seedTestData` — it's the mechanism `MANUAL_TEST_PLAN.md`'s nudge journey *depends on* to validate `NudgeWorker`'s real wiring. If `seedTestData` were broken, a tester sees empty/wrong data immediately and knows something's off. If `debugBackdateLastLog`'s SQL were subtly wrong (wrong row picked, wrong timestamp math), the manual test would silently validate against corrupted state with no visible sign — undermining the one layer this feature still relies on manual testing for. That risk lives entirely in real SQL/Room behaviour, which `NudgeWorkerTest`'s mocked DAOs never exercise.
- Added `NudgeDataTest` (6 tests, new instrumented file, `RoomIntegrationBase`, real in-memory Room — matches this project's existing convention for exactly this class of risk, e.g. `HappyPathTest`/`ExportImportTest`): `getLastLogTimestamp` null with no logs and correctly picks the max among out-of-order inserts (not just the last-inserted row); `getActiveRewardCount` zero with no rewards and correctly excludes archived ones; `debugBackdateLastLog` updates only the most-recent log (a second, older log in the same table is asserted untouched) and is a safe no-op with no logs at all.
- Run for real on a connected device (`./gradlew connectedDebugAndroidTest`, class-filtered to `NudgeDataTest`), not just compiled — 6/6 pass, confirmed via the instrumented JUnit XML report, per `DEV_PLAYBOOK.md`'s "confirm instrumented tests actually run on a device" step.
- `TESTING.md`: `NudgeDataTest` row added under the Repository layer, instrumented test count `~45` → `~50` (rounded, ballpark only — file counts left as-is per this doc's own "rounded, not a maintained tally" guidance).
- Full `ktlintCheck`/`test`/`assembleDebugAndroidTest`/`assembleDebug` re-run clean after the change.

---

### Pass 34 — live-device bug fix + CI regression (same branch)

#### Duplication ✅ (1 fix)
- Fixing the backdate SQL (below) required "hours → ms" math in two new places (`EarnItRepository.debugBackdateLastLog`, `EarnItViewModel.debugGetLastLogIdleHours`), on top of the same `60 * 60 * 1000` literal already appearing twice in `NudgeDecider`'s threshold constants — four copies across the codebase. First attempt centralized it into a `NudgeDecider.MS_PER_HOUR` constant referenced from `EarnItRepository` — see Decoupling below for why that was wrong. Fixed with `java.util.concurrent.TimeUnit` at both new call sites instead: no custom constant, no cross-package dependency. `NudgeDecider`'s own two threshold constants still derive from a local `MS_PER_HOUR`, kept self-contained (not exported).

#### Decoupling ✅ (1 caught before it shipped)
- The first version of the fix above had `EarnItRepository` (`data/`) importing `NudgeDecider` (`nudge/`). `grep`-checked for existing `data/` → `nudge/` imports first (none existed) — this codebase's layering runs the other direction (`NudgeWorker` already imports `EarnItDatabase`/`SettingsRepository`; `data/` has no business knowing `nudge/` exists). Reverted to `TimeUnit` before it landed.

#### Tests ✅ (4 new tests, 1 coverage gap closed, 1 test rewritten to actually reproduce the bug it was meant to catch)
- **The reported bug:** dev-tool taps appeared to do nothing. Diagnosed live against a connected device — pulled the on-device Room DB via `adb ... run-as`, found 5 completion logs within the last 48h; `logcat` confirmed `NudgeWorker` ran successfully both times with no crash. Root cause: `debugBackdateLastLog`'s SQL only rewrote the single most-recent log's timestamp. With "Load full test data" seeding many logs, several sit within seconds of each other — backdating one just promoted the next-newest to the new global max, so the worker correctly saw recent activity and correctly no-opped. Not a worker bug; a dev-tool data bug.
- **Why the existing test didn't catch it:** `NudgeDataTest`'s original case used two logs with ancient, arbitrary timestamps (1000ms/5000ms) and asserted the SQL did what it was designed to do (touch one row) — it never used a dataset shaped like the real bug (several near-simultaneous *recent* logs), so it verified the implementation, not the requirement. Rewrote the case with multiple realistic recent logs; confirmed it fails against the old single-row SQL and passes against the fix (`UPDATE completion_logs SET timestamp = :cutoff WHERE timestamp > :cutoff`) — 6/6 pass, run for real on-device, not just compiled.
- **New coverage gap, closed:** the UX fix added `EarnItViewModel.debugGetLastLogIdleHours` (drives the status text now shown after every tap, specifically so a future silent failure like this one would be visible) with zero test coverage — same blind-spot category as the bug above. Added `NudgeDebugToolsTest` (3 tests, new file, root `com.earnit.app` test package alongside other ViewModel-level tests): whole-hour idle computation from a real timestamp, null when nothing's ever logged, and `debugBackdateLastLog`'s completion callback fires exactly once after the repository write completes — the ordering the simplified 48H/96H buttons depend on to avoid racing `NudgeWorker` against an in-flight DB write.
- **Separate CI regression:** `instrumented-tests` failed on the already-pushed PR (`IllegalStateException: No compose hierarchies found`, ~7 unrelated pre-existing UI tests). Initially misread as the already-documented Android-16-device issue in this doc's Deferrals — ruled out by checking `main`'s CI history (green as of the prior day) and confirming none of the new `Nudge*` tests were among the failures. Actual cause: `MainActivity`'s `POST_NOTIFICATIONS` request (added earlier in this branch) pops a real system dialog on every launch on API 33+, and CI's emulator is API 34 — nearly every UI test launches `MainActivity`. Fixed centrally in `HiltTestRunner.onCreate()` (grants the permission once via `UiAutomation`, before any test `Activity` launches — the same mechanism `GrantPermissionRule` uses internally) rather than adding that rule to ~10 individual test files. Verified against the same API level CI uses: full instrumented suite, 54/54 pass (was ~7 failing).

#### Hardcoded Values ✅ — the `MS_PER_HOUR` finding is covered under Duplication above; no other new magic numbers.

#### Spec Review ✅
- `EARNIT_SPEC.md`'s dev-tooling description and `MANUAL_TEST_PLAN.md`'s steps updated to match the simplified 2-button flow (was 3: two backdate buttons + a separate "CHECK NOW") and the History-timestamp side effect, which is surfaced directly in the debug card's UI, not just the docs — it's the kind of caveat someone could hit with real data present, not only test data.

#### Reviewed, no findings: Naming Consistency, Accessibility, Deprecated APIs, remaining Complexity & Pattern Health items, `git status` (one new untracked file, `NudgeDebugToolsTest.kt`, intentional).

- `./gradlew ktlintCheck`, `test` (141 tests total, all pass), `assembleDebugAndroidTest`, `assembleDebug`, and `connectedDebugAndroidTest` (full suite, 54/54, run on the same API level as CI) all green after every change in this pass — not just the final state.

---

### Pass 35 — `feature/dev-unlock` branch (secret mascot gesture)

#### Duplication ✅
- No composables copy-pasted, no styling patterns repeated inline, no ViewModel/Repository/Dao overlap.
- The new "Sorry, no dev mode at this time" snackbar string is inline in `AboutScreen.kt` rather than in `Strings.kt` — reviewed against what it replaced: the prior "Developer options enabled" string occupied the exact same spot and was also inline, never in `Strings.kt`. Consistent with local precedent at this call site, not new debt.

#### Decoupling ✅ (addressed by design, not found after the fact)
- The tap-timing state machine (group-gap boundary, pause window, reset-on-violation) is exactly the kind of business logic the Decoupling checklist warns against leaving in a composable. Extracted up front into `PugslyGesture`, a plain Kotlin object with no Compose/Android imports — `HomeScreen`'s click handler only calls `nextState`/`isComplete`.
- No ViewModel referencing UI types, no data layer referencing ViewModel/UI concerns.

#### Complexity & Pattern Health ✅ (1 fix)
- **Fixed:** the click handler wrapped `viewModel.enableDevMode()` and `viewModel.bounceMascot()` in a `homeScope.launch { }` coroutine block, but neither call is `suspend` — `enableDevMode()` already launches its own `viewModelScope` coroutine internally, and `bounceMascot()` just calls `tryEmit` synchronously. The wrapping launch was dead ceremony (coroutine scope used where none was needed). Removed; both calls now happen directly in the click lambda.
- `PugslyGesture` has a single caller (`HomeScreen`) — checked against the "does this extraction earn its keep" question: yes, per the Decoupling item above, this is exactly the pure-logic-out-of-composable pattern the checklist asks for, and it's what made the boundary-condition unit tests possible at all.
- Modifier chain on the mascot `Image` (`size → scale → clickable` with a ~10-line lambda) reviewed against `AboutScreen`'s existing version-text `clickable` — same shape and length, consistent with local convention.
- No new/changed `LaunchedEffect` keys. `pugslyTapTimestamps` uses plain `remember`, not `rememberSaveable` — correct: an in-progress secret tap sequence should *not* survive a config change, and it holds no user data.

#### Dead Code & Hygiene ✅
- No unused imports/vars — ktlint's unused-import rule ran clean on every check.
- No commented-out code, no orphaned resources, no stale TODOs.
- `git status` clean apart from the intended new files (`PugslyGesture.kt`, `PugslyGestureTest.kt`).

#### Naming Consistency ✅
- `PugslyGesture.kt` follows the established "bare noun-phrase, pure-logic file" pattern (`WidgetActionButton.kt`, `NudgeDecider.kt`).
- Considered giving it `NudgeDecider`'s treatment — a dedicated top-level package — but `NudgeDecider` earns that because it's consumed by two independent callers (`NudgeWorker`, a debug UI card). `PugslyGesture` has one caller and is specifically a UI-input-gesture concern, closer to `WidgetActionButton.kt` living alongside `widget/`. Kept in `ui/` next to `HomeScreen.kt`.
- Timing constants (`GROUP_GAP_MS`, `PAUSE_MS`, `PATTERN_LENGTH`) live on `PugslyGesture` itself, not `Strings.kt` — they're not display strings, same pattern as `NudgeDecider`'s own threshold constants.

#### Hardcoded Values ✅
- No hardcoded colors. All timing values are named constants on `PugslyGesture`, not inline magic numbers.

#### Accessibility ✅ (1 deliberate trade-off, recorded not fixed)
- Tap target (`150.dp` mascot image) is well over the 48dp minimum.
- `contentDescription` on the mascot image stays `null` even though it's now interactive. This is intentional, not an oversight: giving it a label would announce "this is tappable" to anyone probing the screen (sighted or via TalkBack), which defeats the whole point of this pass — matches how the original About-screen 7-tap trigger was also never labeled as interactive.

#### Deprecated APIs ✅
- No deprecation warnings from any new API used.

#### Spec Review ✅
- Checked `EARNIT_SPEC.md` for any existing description of the dev-mode unlock mechanism — found none; it was never documented there. Nothing to update or contradict.
- Not a listed Deferred Idea.

#### Tests ✅ (10 new tests, 1 gap reviewed and left uncovered with rationale)
- `PugslyGestureTest` (10 tests, new file) covers the pure state machine: group-gap boundary (exact pass, one ms over resets), pause-window boundary (one ms short/over resets, exact min/max accepted), the full 7-tap success path, and two reset scenarios (extra tap before the pause, a slow tap mid-second-burst).
- Reviewed `EarnItViewModel.bounceMascot()` for coverage: it's a one-line wrapper around `_triggerMascotBounce.tryEmit(Unit)`. Checked precedent — `claimReward`'s existing `_triggerMascotBounce.tryEmit(Unit)` emission (this flow already existed before this branch) has never been unit tested anywhere in the codebase; it's a fire-and-forget animation trigger. Left uncovered, consistent with precedent, and testing a single delegating line would just restate the implementation.
- No new `@Inject` site or `AppModule`/`TestAppModule` change — `assembleDebugAndroidTest` not required by the letter of the rule, but re-ran it anyway since the ViewModel changed; graph still builds.
- `TESTING.md` updated: `PugslyGestureTest` row added, unit test count `120+` → `150+`. While updating that line, verified the real number rather than hand-tallying the table — summed the `tests="N"` attribute across every `app/build/test-results/testDebugUnitTest/*.xml` report from the full `./gradlew test` run: 151 total. The "120+" label predates this branch and was already stale by a wide margin; corrected to match the measured count (151, rounded to 150) instead of compounding the drift.
- `MANUAL_TEST_PLAN.md:58` — removed the specific unlock instruction ("7-tap the version number in About") per explicit direction, since it now describes the decoy, not the real mechanism, and documenting the actual gesture in a public repo file would defeat the feature's purpose. No replacement step added describing the new gesture, for the same reason.
- `./gradlew ktlintCheck`, `test` (10/10 new tests confirmed via JUnit XML, full suite green), `assembleDebug` all pass, run sequentially per `CLAUDE.md`.

#### Reviewed, no findings: remaining Duplication/Decoupling items not covered above.

---

### Pass 36 — `chore/cleanup-backlog-fixes` branch (whole-repo audit fixes)

Actioned findings #1, #2, #4–#8 from [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md), a whole-repo audit against this checklist run against `main` at `59d9a82`. Finding #3 (five oversized composables) is deferred to its own branch, `refactor/split-oversized-screens` — comparable in scope to the Pass 10 `EarnItApp.kt` split, and the audit itself recommended not folding it into a hygiene pass. `CLEANUP_BACKLOG.md` stays in the repo, trimmed to that one remaining item, until that branch lands.

#### Duplication ✅ (2 fixes)
- **Fixed:** `TaskDetailScreen.kt` and `RewardDetailScreen.kt` each hand-rolled the LOG pill button (shadow/clip/gradient/border/click/padding) instead of reusing `LogPillButton` from `EarnItButtons.kt` — `HomeScreen.kt` already called it correctly. Both screens now call `LogPillButton` directly; a future LOG-button styling change is one edit instead of three. Visual side effect: both screens' LOG buttons now match `HomeScreen`'s (flat single-tone border/text, more compact padding) instead of each having a slightly different hand-rolled variant.
- **Fixed:** `TaskDetailScreen.kt` hardcoded `"Recent activity"` instead of the existing `Strings.REWARD_RECENT_ACTIVITY`, and hardcoded `"Used in rewards"` / `"Unknown reward"` with no `Strings.kt` entry at all. Added `TASK_DETAIL_USED_IN_REWARDS` and `TASK_DETAIL_UNKNOWN_REWARD`, routed all three through `Strings.kt`. Copy-only, no visible change.

#### Decoupling ✅
- No changes in this pass touched ViewModel/Repository/Dao boundaries.

#### Complexity & Pattern Health — 1 item deferred
- The five oversized composables finding (`RewardDetailScreen.kt` 694 lines, `TaskEditScreen.kt` ~637, `SettingsScreen.kt` ~430, `RewardEditScreen.kt` ~420, `HomeScreen.kt` ~320) is out of scope for this pass — tracked on `refactor/split-oversized-screens` instead.

#### Dead Code & Hygiene ✅
- Removed now-unused `border`/`Brush`/`compositeOver`/`sp` imports from `TaskDetailScreen.kt` and the unused `compositeOver` import from `RewardDetailScreen.kt`, left behind by the `LogPillButton` dedup.
- `git status` clean apart from intended changes.

#### Naming Consistency ✅ (1 fix)
- **Fixed:** `WidgetConfigActivity.kt:211` and `TaskLibraryScreen.kt:97` hardcoded `contentDescription = "Back"` instead of `Strings.BACK_DESC`, despite both files already using `Strings.*` elsewhere. Screen-reader-only string, no visible change.

#### Hardcoded Values ✅ (1 fix)
- **Fixed:** `EarnItPrimaryButton`'s `disabledContainerColor`/`disabledContentColor` were hardcoded to flat greys (`0xFFCCCCCC`/`0xFF999999`) that didn't adapt to Ocean Blue or Forest themes. Checked `DEV_PLAYBOOK.md §4 Known Limitations` first — the only documented hardcoded-disabled-color exception is `LogPillButton`'s warm-gold neutral colors, a different component; this one had no such exception on record. Removed both overrides — Material3's `ButtonDefaults.buttonColors()` already defaults to theme-aware `onSurface.copy(alpha = 0.12f/0.38f)`, so the fix was deletion, not reimplementation.

#### Accessibility ✅ (1 reviewed, documented as exception rather than fixed)
- `InfoIconButton` overrode Compose's default 48dp minimum touch target down to 24dp. Checked for prior rationale first — none found; Pass 1's introduction of `InfoIconButton` only mentions visual consistency, and this wasn't logged as an accepted exception anywhere, unlike `AddTaskToRewardDialog`'s 32dp case (Pass 5). Initially fixed by removing the override, but manual testing on `SettingsScreen`'s name/rewards/tasks toggles caught a visible side effect: Compose's `minimumInteractiveComponentSize()` reports the enlarged 48dp size to the parent layout (not just the touch system), so the row grew taller and pushed a noticeable gap above the info note it reveals. Reverted to 24dp and documented as a permanent, accepted exception in `DEV_PLAYBOOK.md §4 Known Limitations` instead.

#### Deprecated APIs ✅
- No deprecated API usage touched in this pass.

#### Spec Review ✅ (1 fix)
- **Fixed:** `devModeEnabled` was the only `AppSettings` field with no `EARNIT_SPEC.md §6` row — every other field, including more obscure ones, was documented. Added a row describing what it gates (test-data seeding, nudge debug buttons) without naming the unlock trigger. While in the area, fixed a stale comment in `EarnItRepository.kt` and `TestDataSeeder.kt` that still referenced the old "7-tap on About version" trigger, superseded by Pass 35's mascot-gesture unlock.

#### Tests ✅ (1 fix)
- **Fixed:** `TESTING.md`'s instrumented-test table was missing rows for `DuplicateNameUiTest` (2, added Pass 29) and `RewardLimitUiTest` (1, added Pass 29), and `PointFormulaTest`'s row said "(7)" against an actual 9 `@Test` methods (Pass 29 added `difficulty=5`/`preparation=5` bonus cases without bumping the row). Recounted directly via `@Test` grep per file rather than trusting the audit's stated numbers — corrected the audit's own "55 actual" claim to the true figure, 54 (the audit had double-counted a `@TestInstallIn` match in `TestAppModule.kt` as a real `@Test`).

#### Reviewed, no findings: Decoupling, Deprecated APIs — no changes in this pass touched either area.

---

### Pass 37 — CI instrumented-test flake fix + test-isolation hardening (on `refactor/split-task-edit-screen`, scoped to this fix only — not a full-branch pass)

Triggered by a CI-only failure (`autoPoints_slidersUpdateComputedTotal` in `TaskEditScreenUiTest`) on PR #26 that didn't reproduce from local code review. Root cause: `TestAppModule` scopes the in-memory Room database and `SettingsRepository`'s DataStore `@Singleton` on the process-wide Hilt component, so every `@HiltAndroidTest` UI test was leaking its data/settings into whichever test ran after it in the same instrumentation run — several tests (`EmptyStateUiTest`, `SettingsTipUiTest`, `RewardLimitUiTest`) were only passing by luck of execution order. This pass covers only that fix (14 files: the flaky test itself, a new shared reset helper, and its retrofit into every other affected test) — it does not re-review the rest of the branch's earlier TaskEditScreen-split commits.

#### Duplication ✅ (1 avoided)
- `resetAppState()` (new `TestStateReset.kt`) reuses the existing `EarnItRepository.clearAll()` (the repository's `database.clearAllTables()` wrapper, originally written for the app's "Start Over" flow) via a Hilt `@EntryPoint`, instead of re-implementing table clearing.

#### Decoupling ✅
- No changes touched ViewModel/Repository/Dao boundaries; `resetAppState()` lives entirely in the androidTest source set and only adds one new production method (`SettingsRepository.resetToDefaults()`, a thin DataStore wrapper matching the shape of every other `update*` method already on that class).

#### Complexity & Pattern Health ✅
- No composables touched. `resetAppState()` uses `runBlocking`, matching the pattern every retrofitted test's own `@Before` already used for settings setup (`SettingsUiTest`, `RewardLimitUiTest`, etc.).

#### Dead Code & Hygiene ✅
- `git status` clean apart from the intended 14 files (13 modified, 1 new).

#### Naming Consistency ✅
- `TestStateReset.kt` matches the existing androidTest helper naming style (`HiltTestRunner.kt`, `RoomIntegrationBase.kt`) rather than forcing a `*Screen.kt`/`*Test.kt` shape that doesn't fit a plain utility file.

#### Spec Review ✅
- `EARNIT_SPEC.md` only references the instrumented-test count in its testing summary, which this pass doesn't change (no tests added, only isolation fixed between existing ones). No update needed.

#### Tests ✅ (1 gap reviewed, none introduced)
- Reviewed whether `SettingsRepository.resetToDefaults()` needs unit coverage: `SettingsRepository` has no unit tests anywhere in the codebase today (DataStore needs a real `Context`; its existing coverage is entirely instrumented, via `SettingsUiTest`). Consistent with that precedent — left uncovered at the unit level; it's exercised implicitly by every instrumented test's `@Before` now.
- Could not run `connectedDebugAndroidTest` locally (no device/emulator attached this session); verified via `./gradlew assembleDebugAndroidTest` (Hilt graph resolves, all 12 retrofitted files compile against the new `@EntryPoint`) plus `ktlintCheck` and `test`. CI's API 34 emulator is the actual verification path for this change, consistent with the existing Android 16 local-verification gap logged in `TESTING.md` Deferrals.
- Updated `TESTING.md`: added a state-isolation convention note above the instrumented-tests table describing when `resetAppState()` is called and why `RoomIntegrationBase`-based tests don't need it.

#### Reviewed, no findings: remaining checklist items (Hardcoded Values, Accessibility, Deprecated APIs) don't apply — no UI surface in this pass.

---

### Pass 38 — CI flake resolution (`refactor/split-task-edit-screen` branch, follow-up to Pass 37)

Pass 37's isolation fix was real but didn't resolve the original CI failure it was triggered by — `TaskEditScreenUiTest.autoPoints_slidersUpdateComputedTotal` kept failing on GitHub's runners only, never locally. Root cause: the test clicked three sliders back-to-back and checked the combined total once at the end, which both papered over which interaction was wrong and left a window where one click's state update could be missed under CI's slower/differently-scheduled execution. Fixed by asserting after each slider click instead of chaining all three before any check.

#### Tests ✅ (1 fix)
- **Fixed:** `autoPoints_slidersUpdateComputedTotal` now asserts the total after each slider click (verifying each slider maps to its own state variable) instead of clicking all three and checking once. Confirmed green on two independent CI runs after the change.

#### Spec Review ✅
- Added a **Focused coverage** convention note to `TESTING.md` (assert incrementally; don't re-verify logic covered elsewhere) and a matching checklist bullet to this file's `§1 Tests` section, so future tests default to this pattern rather than rediscovering it.

#### Reviewed, no findings: remaining checklist items don't apply — single-test fix plus doc/process additions, no other code touched.

---

### Pass 39 — `refactor/split-settings-screen` branch

Actions [CLEANUP_BACKLOG.md](CLEANUP_BACKLOG.md)'s `SettingsScreen.kt` item (~430 lines in one composable) — same pattern as the `RewardDetailScreen`/`TaskEditScreen` splits: pull screen-only pieces into `private` composables in the same file, no new files/folders, no behavior change.

#### Duplication ✅ (1 reviewed, not fixed)
- `SettingsAppearanceSection`, `SettingsRewardsSection`, and `SettingsTasksSection` each still hand-roll the same section-header `Row` (bold label + optional `InfoIconButton`) instead of extending the existing `SettingsSectionHeader` helper (used as-is by About/Data/Clean Up, which have no trailing icon) with an optional trailing-content slot. Pre-existing duplication, not introduced by this split — left as-is since fixing it changes `SettingsSectionHeader`'s signature, beyond a pure structural split. Worth a follow-up if this file is touched again.

#### Decoupling ✅ (1 fix)
- **Fixed:** `showRewardsInfo`/`optimalText`/`maxText` (Rewards) and `showTasksInfo` (Tasks) were hoisted to the top of `SettingsScreen` even though nothing outside their own section ever read them — pure prop-drilling with no purpose. Moved into `SettingsRewardsSection`/`SettingsTasksSection` themselves. Only `showMascotPicker`/`highlightedMascot` stay hoisted at the orchestrator, since they're genuinely driven from outside (the `viewModel.openMascotPicker` `LaunchedEffect`, fired when a claim unlocks a new mascot).

#### Complexity & Pattern Health ✅
- `SettingsScreen` cut from ~430 lines to a 58-line orchestrator (state + one `LaunchedEffect` + six section calls) plus 8 private helpers: `SettingsAboutSection`, `SettingsAppearanceSection` (which itself calls `SettingsNicknameCard`/`SettingsThemeCard`/`SettingsMascotAndQuoteCard`), `SettingsRewardsSection`, `SettingsTasksSection`, `SettingsDataSection`, `SettingsCleanUpSection`.
- `SettingsNicknameCard`/`SettingsThemeCard`/`SettingsMascotAndQuoteCard` each have exactly one caller (`SettingsAppearanceSection`) — reviewed against "is this extraction earning its keep": yes, this is the same card-per-composable pattern the backlog itself specified, and matches `RewardDetailScreen`'s single-caller helpers from its own split.

#### Dead Code & Hygiene ✅
- No unused imports (ktlint's unused-import rule ran clean on every check).
- `git status` clean apart from the intended 4 files (`SettingsScreen.kt`, `SettingsScreenUiTest.kt`, `TESTING.md`, `CLEANUP_BACKLOG.md`).

#### Naming Consistency ✅
- New composables follow the existing `Settings*` naming already established by `SettingsCard`/`SettingsSectionHeader`/`ThemeChip`/`MascotPickerDialog`/`DangerButton` in this file.

#### Hardcoded Values ✅
- None touched.

#### Accessibility ✅ (1 fix)
- **Fixed:** the "Show daily quote" `Switch` had no `contentDescription` — every other toggle on this screen (Notes Required) already does. Added `contentDescription = Strings.SETTINGS_QUOTE_TOGGLE`, reusing the existing label string, matching the Notes Required precedent exactly. Needed to make the toggle reliably targetable in the new instrumented test, but it's a genuine pre-existing accessibility gap independent of that.

#### Deprecated APIs ✅
- None touched.

#### Spec Review ✅ (2 findings — 1 fixed, 1 flagged not actioned)
- **Fixed:** the Max Reward Count row said only "Hard cap; banner shown when exceeded," which undersold what's actually built and confirmed intended: the FAB dims to 40% opacity at the cap but stays tappable (not a disabled button) — tapping it shows a 2s tooltip instead of navigating, and a separate 3s banner auto-appears the moment a new reward reaches the cap. Row expanded to describe both surfaces and the FAB's non-disabled tappable state.
- **Found while writing spec-grounded tests, not actioned:** `optimalRewardCount` is read back from DataStore and displayed in its own edit field, but is never consumed anywhere else in the app — no banner, no home-screen guidance, nothing reads it besides the Settings field itself. The spec table describes it as "Soft limit; shown as guidance," which isn't true today. Pre-existing (not introduced by this branch), out of scope for a structural-split pass to fix since closing it is a product decision (what should "shown as guidance" look like?), not a mechanical fix — flagged to the user rather than silently patched or silently left in the spec.

#### Tests ✅ (10 new tests, spec-grounded)
- Reviewed instrumented coverage of `SettingsScreen` before writing anything: `SettingsUiTest` (colour scheme persistence, Notes Required) and `SettingsTipUiTest` (discoverability tip) were the only coverage; nickname, random nickname, show-quote, mascot picker, reward-count fields via the UI itself, and the About/Data/Clean Up nav rows had none.
- Added `SettingsScreenUiTest` (10 tests, new file), each grounded in an `EARNIT_SPEC.md` §6 line rather than mirroring implementation: nickname typed in Settings shows in the home greeting; clearing it shows "Earn It!" with no address (the exact spec wording); enabling random nickname overrides the typed name on the greeting ("chosen each session instead of the saved name"); Show Quote toggle hides/shows the daily quote section on Home; Max Reward Count edited through the Settings field itself (not the repository, closing the gap `RewardLimitUiTest` left) still enforces the FAB's max-limit tooltip; the mascot picker's default unlocked set is exactly Pugsly and Tabby per the spec table, with the next-locked mascot's unlock hint shown and further-locked mascots showing neither name nor hint; selecting an unlocked mascot persists after `activityRule.scenario.recreate()`; About/Data & Backup/Clean Up rows navigate to their respective screens.
- One test initially failed on-device (`mascotPicker_defaultUnlockedSet...`): `onNodeWithText("Pugsly").assertIsDisplayed()` found 2 nodes, not 1 — the mascot-picker dialog doesn't unmount the row behind it, and that row already shows "Pugsly" as the current-mascot label. Fixed by asserting via `onAllNodesWithText(...).isNotEmpty()` instead of requiring a single unique match.
- Full instrumented suite run on a connected emulator, not just compiled: 73/73 pass (was 63 before this branch).
- `TESTING.md` updated: `SettingsScreenUiTest` row added; UI test pyramid count `~30` → `~40` (rounded).
- `./gradlew ktlintCheck`, `test`, `assembleDebugAndroidTest`, `assembleDebug`, and `connectedDebugAndroidTest` (full suite) all pass, run sequentially per `CLAUDE.md`.
