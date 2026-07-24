# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 55 — `test/log-for-reward-dialog-multi-reward` branch

Closes the gap Pass 53 found and deferred: `LogForRewardDialog`'s multi-reward branch (`RadioRow`'s other call site, in `TasksScreen.kt`) had no instrumented coverage — every existing test touching that dialog links its task to a single reward, which takes the `else` branch and never renders the reward picker. Added a new instrumented test that links one task to two rewards and drives the picker directly: both reward names render, LOG stays disabled until one is selected, and logging credits only the chosen reward. Test-only change — no production code touched.

#### Duplication ✅ — found and fixed
Two separate things checked here, not one: (1) The two reward-creation-and-link blocks are inlined in a loop rather than extracted into a new `UiTestActions.kt` helper — verified against the file directly (only `createTask`, `createReward` with no task-link, `waitForTaskDetail`, `waitForRewardDetail` exist there) and against `TESTING.md`'s documented convention that shared helpers cover the common create-and-save path only, flows needing an inline reward-task link build the steps inline, matching `SharedDialogsCancelUiTest.kt`'s existing precedent for the same block. (2) The first draft of this test repeated `hasAnyAncestor(isDialog()) and hasText(...)` verbatim three times within the one test method — missed on the first pass through this checklist, caught on a second, more literal read of the diff. Extracted a private `ComposeTestRule.dialogNodeWithText(text)` local to the file (3 call sites, so it earns its keep) rather than adding it to `CancelDismissAssertions.kt`, since nothing outside this file needs it yet.

#### Decoupling / Naming Consistency / Hardcoded Values / Accessibility / Deprecated APIs ✅ (n/a)
Test-only change — no ViewModel, Repository, Dao, or Compose UI code touched. New file follows the existing `*UiTest.kt` naming and package-root placement.

#### Complexity & Pattern Health ✅ (checked)
`dialogNodeWithText` (added during the Duplication fix above) has 3 call sites in this file, not 1 — not a premature single-caller extraction.

#### Dead Code & Hygiene ✅
`./gradlew ktlintCheck` clean. `git status` clean apart from the intended files.

#### Spec Review ✅ (checked, no changes needed)
Grepped `EARNIT_SPEC.md` for `LogForRewardDialog|multi-reward|RadioRow` — no hits. No behavior changed, only test coverage added, so there's no contract to reconcile.

#### Tests ✅ (1 new file, 1 new test)
- New file `LogForRewardDialogUiTest` (1 test), tagged `@UiTest @Task @Reward`. Kept as its own file rather than folded into `SharedDialogsCancelUiTest.kt` despite being under `CLEANUP_RULES.md`'s 3-test new-file guideline: that class's doc comment and `TESTING.md` entry specifically scope it to cancel/dismiss paths, and this is a confirm-path test covering a genuinely distinct behavior.
- Ran on the connected emulator before committing, per the checklist — caught a real bug on the first run: asserting on reward names by plain text matched twice, because `TaskDetailScreen`'s background "Used in Rewards" list repeats the same reward names behind the dialog. Fixed by scoping the picker's node lookups to the dialog's own window (`hasAnyAncestor(isDialog()) and hasText(...)`), mirroring the existing pattern in `CancelDismissAssertions.kt`. Passed after the fix.
- Re-ran on the emulator a second time after the Duplication-fix refactor above (extracting `dialogNodeWithText`) — an unrelated import (`onNode`) briefly broke the build (`onNode` is a `SemanticsNodeInteractionsProvider` member, not a top-level import, same as `CancelDismissAssertions.kt` already relies on); fixed and passed.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required.
- `./gradlew ktlintCheck` and `test` both pass.
- `TESTING.md`: added a table row for `LogForRewardDialogUiTest`; extended the existing "Task attached to two rewards simultaneously" edge-case entry to reference the new UI-level coverage. Aggregate counts unchanged (one test doesn't move the rounded figures).
- `QA_AUDIT_BACKLOG.md`: checked — this gap was never logged there (it lived only in Pass 53's log entry), so nothing to remove.

---

### Pass 56 — `fix/edge-to-edge-deprecated-apis` branch

Play Console flagged two edge-to-edge warnings on upload: a generic "may not display for all users" notice, and a deprecated-API list (`Window.setStatusBarColor`, `Window.setNavigationBarColor`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`) attributed to obfuscated call sites (`se0.a`, `re0.b`). `enableEdgeToEdge()` was already called correctly in `MainActivity.onCreate()` and `Scaffold` already threads system-bar insets through as content padding, so the app's own Compose insets handling wasn't the problem. Root-caused to two things instead: `themes.xml` still set `android:statusBarColor`/`android:navigationBarColor` explicitly (redundant now, and one of the deprecated items Google's own edge-to-edge migration guide says to delete), and `androidx.activity:activity-compose` (1.9.0), `androidx.core:core-ktx` (1.12.0), and `com.google.android.material:material` (1.13.0) were multiple minor versions behind the rest of the stack — all three are named in public GitHub issues as sources of these exact deprecated calls inside their own `enableEdgeToEdge()`/`EdgeToEdgeUtils` internals. Removed the two theme attrs and bumped all three dependencies to current stable (1.13.0 / 1.18.0 / 1.14.0 respectively — `core-ktx` capped at 1.18.0 rather than 1.19.0 because 1.19.0's AAR metadata requires `compileSdk 37`, and bumping `compileSdk`/`targetSdk` was explicitly out of scope for this pass).

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility ✅ (n/a)
Config-only change (one XML theme file, one Gradle version catalog) — no Kotlin, Compose, ViewModel, Repository, Dao, or resource files touched, no new files added.

#### Dead Code & Hygiene ✅
`git status`/`git diff --stat` confirm exactly the 2 intended files changed (3 insertions, 5 deletions), nothing stray. No resource became orphaned by removing the two theme attrs — `?attr/colorPrimary`/`?attr/colorSurface` are Material3 theme attributes still used throughout the app's color scheme, not resources that only these two lines referenced.

#### Deprecated APIs ✅ — the point of this pass
Removed `android:statusBarColor`/`android:navigationBarColor` from `themes.xml`; `enableEdgeToEdge()` (already in place) owns bar appearance now. Bumped the three dependencies most plausibly responsible for the library-internal deprecated calls. Caveat carried forward, not fully closed: this is a widely-reported issue (Flutter, React Native, and native apps alike hit the same three deprecated symbols from AndroidX/Material internals) with no confirmed fully-clean release as of this pass — Play Console's warning may persist even after this fix until upstream fully removes the calls. It's an advisory warning, not a publish blocker.

#### Spec Review ✅ (checked, no changes needed)
Grepped `EARNIT_SPEC.md` for `edge-to-edge|status bar|statusBar|navigationBar|colorPrimary|colorSurface` — the one hit (`nav bar` badge behavior) refers to the in-app bottom navigation bar, unrelated to the system status/navigation bars this pass touches. No documented behavior to reconcile.

#### Tests ✅ (0 new test files — config/resource-only change)
- No unit-testable logic changed; this is a theme resource + dependency-version change with no JVM-reachable code path.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`. Also ran `./gradlew assembleDebugAndroidTest` even though `AppModule`/`TestAppModule`/`@Inject` weren't touched, as extra insurance given the dependency bumps sit underneath Activity/Hilt integration — passed clean.
- Checked `MANUAL_TEST_PLAN.md`'s scope against this change: its entries are all cross-process-boundary *flows* (file picker, widget activity chain, background worker) that instrumented tests structurally can't drive. Status/nav-bar rendering across OS versions and themes is a visual/system-rendering check, not a flow of that kind, so a permanent entry doesn't fit — handled instead as a one-time manual visual check on this branch before merge (light/dark, Ocean Blue/Forest themes).
- `TESTING.md`: no changes needed — no test added, removed, or renamed.

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
