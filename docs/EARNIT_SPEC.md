# EarnIt — App Specification

## Overview

**EarnIt** is a local-only Android app that gamifies productivity. Users earn points by completing tasks and spend them to unlock personal rewards. Each reward tracks its own point balance independently. A mandatory task gatekeeper ensures accountability before any reward can be claimed. Every claimed reward is permanently archived in a History with a full log of the tasks that funded it.

---

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Theme | 3 colour schemes (Warm Gold, Ocean Blue, Forest) |
| Widget | Jetpack Glance |
| Storage | Room (SQLite, schema v10) — local only, no cloud |
| DI | Hilt |
| Navigation | Navigation Compose |
| Settings | DataStore Preferences |
| Min SDK | API 31 (Android 12) |

---

## Architecture

**MVVM** throughout:
- `ViewModel` + `StateFlow` drive all UI state
- Room DAOs are the single source of truth
- `EarnItRepository` computes derived state (`EarnItUiState`) via `combine` on multiple Flows
- Hilt provides DI across all layers
- Navigation Compose handles screen routing
- Glance handles the home screen widget independently

---

## 1. Task Engine

### Task Fields

| Field | Type | Notes |
|---|---|---|
| Name | String | Required |
| Icon | String (emoji) | Optional |
| Group | String? | Optional label for grouping tasks. Null = appears in "Other" in group view. |
| Repeatable | Boolean | Default true. If true, can be logged multiple times. |
| Points | Int | Default 4. Used when `useAutoPoints = false`. |
| Use Auto Points | Boolean | Default false. If true, ignores manual points; uses formula. |
| Time | Int (1–5) | Slider. Only relevant when `useAutoPoints = true`. |
| Difficulty | Int (1–5) | Slider. Only relevant when `useAutoPoints = true`. |
| Preparation | Int (1–5) | Slider. Only relevant when `useAutoPoints = true`. |
| Sort Order | Int | Controls display order; managed by drag-to-reorder. |

### Auto-Point Formula

```
Base   = ceil((Time+1) × (Difficulty+1) × (Preparation+1) / 8)
Bonus  = +3 if any dimension equals 5, otherwise 0
Points = Base + Bonus
```

Range: min **1** (all sliders at 1), max **30** (all sliders at 5).
All three dimensions interact multiplicatively — a task that is long, hard, *and* requires preparation earns disproportionately more than any single dimension alone. Pushing any single dimension to its maximum (5) grants an additional +3 bonus.

Implemented as integer ceiling division:
`((time + 1) * (difficulty + 1) * (preparation + 1) + 7) / 8 + if (maxOf(time, difficulty, preparation) == 5) 3 else 0`.

### Completion Log

Every task completion creates a `CompletionLogEntity` recording:
- Task ID and name (name snapshotted at log time)
- Reward ID (the reward the log is attributed to)
- Timestamp
- Detail — optional free-text note
- Points — computed at log time from formula or manual value
- History Entry ID — null while active; set when the reward is claimed

---

## 2. Per-Reward Point Pool

Each reward maintains its own independent point balance via its completion logs. Points are **not** shared globally across rewards.

- Logging a task completion is always attributed to a specific reward.
- A reward's balance = sum of points on its active (unclaimed) logs.
- Claiming a reward archives all its active logs under a History entry.
- Excess logs beyond the reward's cost are archived in full — there is no partial attribution.

---

## 3. Reward Store & Per-Reward Gatekeeper

### Reward Fields

| Field | Type | Notes |
|---|---|---|
| Name | String | Required |
| Icon | String (emoji) | Optional |
| Description | String | Optional motivational text |
| Point Cost | Int | Required |
| Is Archived | Boolean | True after claiming without "start over" |
| Sort Order | Int | Controls display order |
| Created At | Long | Timestamp set on first insert |

### Task Links (RewardTaskCrossRef)

Each reward can have tasks linked to it with two flags per link:

| Flag | Meaning |
|---|---|
| `isMandatory` | Task must be logged at least once before the reward can be claimed |
| `isRepeatable` | Task can be logged multiple times toward this reward |

### Gatekeeper Logic

The "Complete to earn points" task list on Reward Detail shows mandatory tasks first (A→Z), then optional tasks (A→Z).

The **"Earned It"** button is disabled unless:
1. The reward's accumulated points ≥ its cost, AND
2. Every mandatory task has been logged at least once since the last claim

Mandatory tasks reset only when their gating reward is claimed.

### Progress & Feedback Animations

Reward progress cards (home screen and Reward Detail) include live animated feedback:

| Trigger | Feedback |
|---|---|
| Points change | Progress bar animates smoothly to new value (`animateFloatAsState`, 500 ms, `FastOutSlowInEasing`) |
| Task logged | Floating `+X` label rises from the progress bar and fades out (`Animatable` offset + alpha, triggered by `LaunchedEffect` on point increase) |
| Progress hits 100% | Gradient border pulses (`infiniteRepeatable` alpha 0.5→1.0, 900 ms) and CLAIM button gently scales 1.0→1.05 on a 700 ms loop |

**Haptics:** every primary action (LOG, CLAIM, Save Reward, Save Task) fires a short `VIRTUAL_KEY` haptic via `LocalView.current.performHapticFeedback`. No permission needed; respects the device system setting automatically — no in-app toggle.

### Claiming Flow

1. All mandatory tasks logged + points met → "Earned It" activates.
2. User taps "Earned It" → confirmation dialog with two options:
   - **Start over** — reward stays active, all logs archived, balance resets to 0
   - **Archive** — reward is marked archived and removed from active view
3. All active logs for that reward are archived into a History entry.

---

## 4. History

When a reward is claimed, a `HistoryEntryEntity` is created:

| Field | Notes |
|---|---|
| Reward ID | Reference to original reward |
| Reward Name | Snapshotted at claim time |
| Reward Icon | Snapshotted at claim time |
| Point Cost | Snapshotted at claim time |
| Claimed At | Timestamp |

All active logs for the reward are archived by setting their `historyEntryId` to this entry's ID.

### Display

Each History entry shows:
1. Reward name, icon, and date/time claimed
2. Chronological list of all contributing logs (task name, note, timestamp, points)

A "Reactivate" option allows copying an archived reward back to the active reward list, preserving its task links.

---

## 5. Jetpack Glance Widget

### Variants

One entry in the home screen widget picker:

| Variant | Size | Description |
|---|---|---|
| Standard | 3×1 | Reward name + progress bar + log / CLAIM button |

### Display States

- **Active:** reward name, progress bar, log or CLAIM button depending on state
- **Done ✓:** all tasks logged but points still short — no button shown
- **Claimed / archived:** reward name + "Earned and Claimed" subtitle; tapping opens the app
- **Empty:** "Long-press to configure" — reward not yet selected
- Tapping anywhere on the widget body (outside buttons) opens the app to the tracked reward

### Configuration

`WidgetConfigActivity` is launched on widget placement (and from long-press):
1. **Pick reward** — lists all active rewards; user taps to select
2. **Edit label** — text field pre-filled with the reward name; user can change it to anything (e.g. "Personal Goal") for privacy. The label is what appears on the home screen widget — the actual reward name in the app is unchanged.
- Reward ID and custom label persisted in widget prefs (`widget_reward_id`, `widget_reward_name`)

### Task Logging from Widget

`WidgetTaskLogActivity`:
1. `TaskPickerScreen` — lists all loggable tasks for the tracked reward
2. Note input screen — optional free-text detail
3. On confirm: shows in-activity success screen (✓, task name, +X pts) for 1.5 seconds, then auto-closes. On close: triggers haptic, fires local notification ("Task name / Logged! +X pts"), writes log to DB, updates widget

### Widget Theme

Colors follow the app's selected color scheme (Warm Gold / Ocean Blue / Forest) and respect system dark mode. Colors are resolved at draw time from `ColorSchemes.lightColors` / `darkColors`.

`WidgetTaskLogActivity` and `WidgetConfigActivity` both read the user's selected scheme via `viewModel.settings.collectAsState()` and pass it explicitly to `EarnItTheme` — without this, the widget activities would always render in Warm Gold.

### Celebratory Feedback

- **Haptic:** 60ms vibration on task log from widget
- **Flash:** widget shows "✓ Logged!" for 3 seconds after a log, then reverts to normal. Revert is self-contained in the widget's Glance composition via a `produceState` timer — survives app process death (e.g. APK reinstall). `WidgetFlash` stores the expiry timestamp in SharedPrefs; `remainingMs()` lets the timer fire precisely when the flash expires.
- **Notification:** "Task name / Logged! +X pts" system notification; requires `POST_NOTIFICATIONS` on Android 13+
- **In-activity confirmation:** success screen (✓, task name, +X pts) shown for 1.5 s inside `WidgetTaskLogActivity` before auto-closing

---

## 6. App Settings

Settings are persisted via DataStore Preferences.

| Setting | Type | Default | Notes |
|---|---|---|---|
| Colour Scheme | Enum | `WARM_GOLD` | `WARM_GOLD`, `OCEAN_BLUE`, `FOREST` |
| Notes Mandatory | Boolean | false | When true, a note is required on every log |
| Optimal Reward Count | Int | 3 | Soft limit; shown as guidance |
| Max Reward Count | Int | 7 | Hard cap; banner shown when exceeded |
| Nickname | String | `"Babe"` | Displayed in the home screen greeting. Clearing the field stores `""` — greeting shows "Earn It!" with no address. |
| Use Random Nickname | Boolean | false | When true, a random fun nickname is chosen each session instead of the saved name |
| Selected Mascot | MascotId? | `PUGSLY` | Active mascot shown on the home screen; `null` = hidden. Migrated from legacy `show_pugsly` boolean. |
| Unlocked Mascots | Set\<MascotId\> | `{PUGSLY, TABBY}` | Set of mascots the user has earned. Grows as milestones are hit; never shrinks. |
| Show Quote | Boolean | true | Shows or hides the daily quote on the home screen |
| Tasks Group View | Boolean | false | When true, the Tasks screen shows tasks in collapsible group sections instead of a flat list |

### Colour Schemes

| Scheme | Primary | Secondary | Surface |
|---|---|---|---|
| Warm Gold | `#E8A000` amber | `#2A9D8F` teal | `#FFFBF0` warm cream |
| Ocean Blue | `#1976D2` blue | `#0097A7` cyan | `#F2F7FF` cool white |
| Forest | `#2E7D32` green | `#795548` brown | `#F6F3EE` warm off-white |

Each scheme has a light and dark variant. Mascot images are a single PNG each — no per-scheme tinting.

---

## 7. Export / Import / Backup

### Auto Backup

Android automatically backs up the app's Room database (`earnit.db`) and DataStore preferences to the user's Google account daily (when charging + on Wi-Fi). Configured via `android:dataExtractionRules="@xml/data_extraction_rules"` in the manifest. Restores automatically when the app is installed on a new device with the same Google account — no user action needed.

### Export

Serialises the entire database to JSON (via Moshi) and saves to a user-chosen file location via `ActivityResultContracts.CreateDocument`. Suggested filename: `earnit_backup_YYYY-MM-DD.json`. The user can save to Downloads, Google Drive, email it to themselves, etc.

JSON format:

```json
{
  "tasks": [...],
  "rewards": [...],
  "rewardTaskCrossRefs": [...],
  "completionLogs": [...],
  "historyEntries": [...]
}
```

### Import

User picks a `.json` file via `ActivityResultContracts.GetContent`. Two modes:

| Option | Behaviour |
|---|---|
| Replace all data | Calls `database.clearAllTables()` then re-inserts everything |
| Merge | Inserts with `IGNORE` conflict strategy — existing records (same ID) are preserved |

---

## 8. Task Templates (Library)

Three built-in templates accessible via **Tasks → Library**:

| Template | Tasks |
|---|---|
| 🌿 Healthy Living | Morning Run, Workout, Yoga Session, Meditate, Cold Shower, Cook Healthy Meal, Drink 2L Water, Sleep 8 Hours, Evening Walk, Stretch |
| 🤝 Social | Call a Friend, Family Dinner, Help Someone, Send a Thank You, Plan a Social Event, Volunteer, Write a Letter, Check In on Someone, Host a Friend, Join a Club or Group |
| 🏠 Clean Home | Clean Room, Do Laundry, Wash Dishes, Vacuum, Clean Bathroom, Take Out Trash, Organize a Drawer, Wipe Down Surfaces, Mop Floors, Declutter |

Each template card is collapsible. Individual tasks can be deselected before importing. The ADD button imports only the selected tasks. Imported tasks are automatically assigned the template name as their group. If any selected tasks share a name with an existing task (case-insensitive, whitespace-trimmed), they are skipped; a dialog lists the skipped names after import so the user knows what was not added.

---

## 9. Tests

See [TESTING.md](TESTING.md) for the full picture — current coverage, known gaps, and what to write next.

**Summary:** 79 unit tests across 13 test files. 26 instrumented tests across 9 files (requires device/emulator) — including 4 Compose UI tests.

---

## 10. Screen Map

```
Main App
├── Prizes (Home)            — active rewards, progress cards, mascot, quote of the day
│   ├── Reward Detail        — tasks checklist, progress bar, Earned It / claim flow; "Add task" button inside the "Complete to earn points" section opens AddTaskToRewardDialog for immediate linking
│   └── Reward Edit          — name, icon, description, cost, linked tasks (mandatory/repeatable flags); duplicate name blocked with inline error on the name field; shows "Reward saved" snackbar on save; Save does not auto-navigate back
├── Tasks                    — draggable task list; group view toggle (flat / collapsible groups); Library shortcut
│   ├── Task Detail          — task info (name, icon, group chip, points), linked rewards, log history
│   ├── Task Edit            — name, icon, group, repeatable toggle, manual or auto points (sliders); group field is a collapsible section (expanded by default, collapses to "Group · [name]" summary) with a bordered radio-button list of existing groups; "New group..." is the last row in the list — a radio + inline text field; selecting a radio clears the new-group field and vice versa; duplicate name blocked with inline error on the name field; shows "Task saved" snackbar on save; Save does not auto-navigate back
│   └── Task Library         — 3 collapsible template cards with per-task checkboxes; imported tasks auto-assigned to template group; tasks with duplicate names are skipped and listed in a post-import dialog
├── History                  — claimed rewards tab + completed tasks tab
└── Settings
    ├── About                — app name, version, "The idea" copy; Rate the app (opens Play Store); Get in touch (email); Support the developer (Tip Jar — two price buttons, loading state, thank-you snackbar; mock until RevenueCat integrated)
    ├── Appearance           — nickname input + random-nickname toggle, Mascot picker, Show Quote toggle
    ├── Colour Scheme        — 3 theme chips (Warm Gold, Ocean Blue, Forest)
    ├── Reward Limits        — optimal count, max count inputs
    ├── Tasks                — notes required toggle
    ├── Data & Backup        — export JSON, import JSON (replace or merge)
    └── Clean Up             — separate cards: clear logs / clear tasks / clear rewards / clear all; each operation shows a snackbar confirmation after completing; "Clear Logs" deletes both completion logs and all history entries

Widget (Glance)
├── Progress display         — custom widget label + balance/cost bar
├── + LOG button             — opens WidgetTaskLogActivity
├── CLAIM button             — opens MainActivity at reward detail
└── WidgetConfigActivity     — two-step: reward selection → editable label
```

---

## Deferred Ideas

Ideas explicitly kept out of current scope. Revisit when the core is stable.

---

### Tip Jar (In-App Support) — deferred, UI hidden behind feature flag

**Status:** UI complete, hidden. Set `FeatureFlags.TIP_JAR_ENABLED = true` in `FeatureFlags.kt` to restore the About screen section. Deferred until merchant account is available (requires non-home address).

**Context:** External "Buy Me a Coffee" links violate Google Play policy. Voluntary IAP tips are permitted under Play's monetisation policy (consumable, no in-app reward). Google takes 15% (small business rate).

**What is built:**
- `TipRepository` interface + `MockTipRepository` in `data/` — returns fake prices; real RevenueCat implementation slots in here
- `TipViewModel` in `viewmodel/` — `TipState` (Loading / Ready / Error), `PurchaseEvent` SharedFlow for snackbar feedback
- `AboutScreen` "Support the developer" section — loading indicator, two price buttons, thank-you/error snackbar (gated by `FeatureFlags.TIP_JAR_ENABLED`)
- `AppModule` provides `MockTipRepository` as `TipRepository` — swap to real impl here when RevenueCat is set up

**What remains (RevenueCat integration):**
1. Set `FeatureFlags.TIP_JAR_ENABLED = true`
2. Create Play Console IAP products: `tip_small` (Tiny Tip) and `tip_coffee` (Coffee Tip) — mark both Active
3. Create RevenueCat project; connect Play app; add products to an Offering called `tips`
4. Add `com.revenuecat.purchases:purchases` to `build.gradle.kts`
5. Initialise `Purchases.configure(...)` in `EarnItApplication.onCreate`
6. Write `RevenueCatTipRepository : TipRepository` — calls `Purchases.getOfferings()` for prices, `Purchases.purchase()` for transactions
7. In `AppModule`, replace `MockTipRepository` with `RevenueCatTipRepository`
8. Update `Strings.ABOUT_CONTACT_EMAIL` with real SecondMonday Studios address before release

**Policy reminder:** Prices must always be loaded dynamically from RevenueCat — never hardcode `$2` or `$5` in the UI.

---


---