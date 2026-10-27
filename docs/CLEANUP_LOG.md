# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 74 — `feat/about-disclaimer` branch

Added a short liability disclaimer ("as is," no warranty, not liable for data loss/damages) to the About screen, directly below the Privacy policy row, so the app carries the same baseline protection bigger free apps ship with.

#### Duplication / Decoupling / Hardcoded Values / Deprecated APIs / Dev Seed Data — n/a
Two new `Strings.kt` constants and one new static text `Column`, copied structurally from the existing "The idea" section (same `Column`/`Text` pattern, `HorizontalDivider()` separator) — no new abstraction, no business logic, no colors/durations introduced.

#### Complexity & Pattern Health ✅ (checked, pre-existing issue noted, not fixed)
`AboutScreen`'s top-level composable was already ~200 lines before this change (over the ~150-line split guideline) — this pass adds 16 lines to it rather than introducing the overage. Not restructured here: splitting it is a separate refactor with its own blast radius, out of scope for a copy-only addition.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only the 5 intended files changed. No unused imports — the new code reuses `Column`/`Text`/`HorizontalDivider`/`MaterialTheme` already imported in the file.

#### Naming Consistency ✅ (checked)
`ABOUT_DISCLAIMER_TITLE`/`ABOUT_DISCLAIMER_BODY` follow the existing `ABOUT_*` naming and sit next to `ABOUT_PRIVACY_*` in `Strings.kt`.

#### Accessibility ✅ (checked, n/a)
Static text, not tappable — no content description or touch-target sizing applies.

#### Spec Review ✅ — updated
`EARNIT_SPEC.md`'s About screen tree line didn't mention the Privacy policy link either; added both Privacy policy and Disclaimer to that line so it now matches the actual screen contents.

#### Tests ✅ — added, ran on device
Added `aboutScreen_displaysDisclaimer` to `SettingsScreenUiTest.kt`, mirroring the existing `aboutScreen_doesNotClaimDataNeverLeavesThePhone` regression-style assertion. Ran the full file on a connected emulator before committing: 15/15 passed. `TESTING.md`'s `SettingsScreenUiTest` row count (14→15) and its UI-layer pyramid figure (87→88) updated to match; the instrumented-aggregate "120 tests" headers stay as-is (already an intentional round-to-nearest-10, per Pass 73). No `AppModule`/`TestAppModule`/`@Inject` change, so `assembleDebugAndroidTest` wasn't required beyond the `connectedDebugAndroidTest` run. `./gradlew ktlintCheck` and `assembleDebug` both pass.

---

### Pass 75 — `chore/dependency-version-bump` branch

Google Play compliance/upgrade audit found `androidx.lifecycle` (2.6.2) and `androidx.navigation:navigation-compose` (2.7.3) in `gradle/libs.versions.toml` several versions behind the rest of the catalog. Bumped `lifecycle` → 2.10.0, `navigationCompose` → 2.9.8, and `work` → 2.12.0 (this last one wasn't stale, just one minor release behind — bumped while touching the file).

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility / Dev Seed Data — n/a
Version-catalog-only change, no source code touched.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only `gradle/libs.versions.toml` and docs changed.

#### Deprecated APIs ✅ — investigated, documented as a version ceiling
Latest stable `lifecycle` (2.11.0) and `navigation-compose` (2.10.2) both require `compileSdk 37`, which fails `checkDebugAarMetadata` against this project's `compileSdk 36`. Took the highest versions still compatible with `compileSdk 36` (2.10.0 and 2.9.8 respectively) instead, mirroring the existing `core-ktx` 1.18.x constraint. Documented as a new gotcha (#7) in `DEV_PLAYBOOK.md`'s Tooling Upgrade Reference so the next bump doesn't rediscover it by trial and error.

#### Spec Review ✅ (checked, n/a)
No behavior change — `EARNIT_SPEC.md` doesn't document dependency versions.

#### Tests ✅ (checked, n/a)
No source change, so no new test warranted. Ran the full pre-merge gate sequentially (never in parallel, per `CLAUDE.md`'s Windows Kotlin-daemon note): `./gradlew ktlintCheck`, `./gradlew test` (passed), `./gradlew assembleDebug` (passed), `./gradlew assembleDebugAndroidTest` (passed — confirms the Hilt graph still resolves against the bumped `hilt-navigation-compose`/`hilt-work` transitive deps).

---

### Pass 76 — `fix/widget-reliability` branch

Two widget correctness bugs, reported by the user: (1) the in-app "Add widget" button on Reward Detail (`requestPinAppWidget`) placed the widget but relied on the launcher to auto-invoke `WidgetConfigActivity` afterward, which isn't guaranteed by the platform — some launchers never ran it, leaving the widget permanently stuck unconfigured with no way to recover (long-press on an already-placed widget only offers resize/remove). (2) a widget tracking a reward that gets hard-deleted (not claimed) showed "Earned and Claimed" instead of reflecting that the reward is gone, because both cases produce an identical `progress == null` with a non-zero stored `rewardId`.

Fix for (1): removed the in-app button entirely rather than patching around the unreliable API — the regular "long-press → Widgets → EarnIt" add flow already works and is already the one covered by the manual test plan. Fix for (2): added a `rewardExists` check and a new "Reward removed" widget state, distinct from both "claimed" and "never configured."

**Regression in the first attempt, caught by the user re-testing manually (not by the automated gate):** the first version of `rewardExists` used `produceState(key1 = rewardId) { value = repo.getRewardOrNull(rewardId) != null }` — a one-shot `suspend` lookup. `produceState`'s producer block only re-runs when its key changes; since `rewardId` doesn't change when the reward is later deleted, the value computed at widget-configure time (`true`) was never re-read, so the widget kept showing "Earned and Claimed" forever instead of flipping to "Reward removed." Fixed by adding `RewardDao.observeReward(id): Flow<RewardEntity?>` and `EarnItRepository.observeRewardExists(id): Flow<Boolean>`, and collecting that Flow inside `produceState` (the same live-collection pattern `allProgress`/`scheme` already use in this same function) instead of doing a one-shot query.

#### Duplication / Decoupling / Naming Consistency / Hardcoded Values / Deprecated APIs ✅ (checked, nothing found)
No duplication introduced. `WidgetViewStateTest.kt` sits alongside `WidgetContentTest.kt` in the same package, matching existing naming/placement. The two new literal strings ("Reward removed", "Remove and re-add this widget") are inline in `EmptyState` rather than `Strings.kt`, consistent with this composable's existing sibling literals ("EarnIt", "Earned and Claimed", "Logged!") — not migrated to `Strings.kt` as part of this fix, to keep the change scoped; flagging here rather than silently leaving it unconsidered.

#### Complexity & Pattern Health ✅ (checked, one-caller helper justified)
`resolveWidgetViewState` currently has a single caller (`WidgetContent`) — normally a flag per the "does a new helper with only one caller earn its keep" checklist item, but here the extraction's entire purpose is to pull the exact branching logic the bug lives in out from behind `provideGlance`'s untestable Hilt/Room/Glance pipeline so it can be unit-tested directly; inlining it back would remove that coverage.

#### Dead Code & Hygiene ✅ (checked and fixed)
Removed now-dead code the button removal left behind: `hasEarnItWidgetPinned`/`requestPinEarnItWidget` (`EarnItWidget.kt`), `widgetNudgeDismissed` (`AppSettings.kt`/`SettingsRepository.kt`), `dismissWidgetNudge()` (`EarnItViewModel.kt`), the `WIDGET_NUDGE_*` `Strings.kt` constants, `DismissibleTipBanner` (its only caller), and the now-unused `AppWidgetManager`/`ComponentName`/`LocalContext`/`Column`/`height` imports it left behind — caught by `ktlintCheck` after the first pass of edits. Deleted `WidgetNudgeUiTest.kt` (androidTest), which tested only the removed flow.

#### Accessibility ✅ (checked, n/a)
`DismissibleTipBanner`'s dismiss `IconButton` (with content description) was removed along with the feature, not left dangling.

#### Spec Review ✅ — updated
`EARNIT_SPEC.md`: removed the "Widget Nudge" section; updated the widget Display States list to add "Reward removed" and correct "Empty" state's copy. Test-count summary line recomputed and rounded: 220→225 unit tests, 28→29 files (new `WidgetViewStateTest.kt`); instrumented aggregate unchanged (123 actual still rounds to 120).

#### Tests ✅ — added regression coverage, ran full gate
Added `WidgetViewStateTest.kt` (5 cases) covering `resolveWidgetViewState` as a pure function — correctly caught the claimed-vs-removed branching logic, but could not and did not catch the one-shot `produceState` regression above, since that bug is entirely in the wiring that computes `rewardExists`, not in what it does with the result. The actual regression test for that is in `DeleteCascadeTest.kt`: `observeRewardExists reflects live changes to the underlying row, not just its value at subscribe time` — stubs `rewardDao.observeReward` with a `MutableStateFlow`, asserts `true`, flips the row to `null`, asserts `false`, which would have failed against the one-shot version. The Compose/Glance recomposition layer above that (does `provideGlance` actually re-render when the Flow emits) is still only verified by the manual test plan step, consistent with this project's established position that `provideGlance`'s pipeline is outside unit-test reach. Updated `WidgetContentTest.kt`'s `emptyState_showsConfigurePrompt` for the new subtitle copy and added a case for `EmptyState`'s new `title` param (12→14 tests). Deleted `WidgetNudgeUiTest.kt` rather than leaving it passing against dead code. Updated `MANUAL_TEST_PLAN.md`: removed the Add Widget button step, added a reward-removed spot-check. Updated `TESTING.md`'s pyramid/section-header counts and per-file table rows. Ran the full gate sequentially after both the initial fix and the correction: `./gradlew ktlintCheck`, `./gradlew test` (227/227), `./gradlew assembleDebug`, `./gradlew assembleDebugAndroidTest` (all passed — the new `RewardDao.observeReward` method changes the Room schema's Dao surface, so `assembleDebugAndroidTest` was worth the re-run beyond just policy).

#### Dev Seed Data ✅ (checked, n/a)
`TestDataSeeder` only seeds Room data (rewards/tasks/logs); the widget-reward binding this bug is about (`widget_reward_id`/`widget_reward_name`) lives in Glance's own per-widget-instance Preferences DataStore, which the seeder has no access to and can't set up regardless of what Room rows exist. Nothing to add here — the manual test plan step added above is the only way to reach this scenario.
