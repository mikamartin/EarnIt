# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 53 — `fix/task-edit-group-picker-style` branch

Started from a design-review request: Task Edit's group-field radio list used a bordered, transparent-fill box (`border(1.dp, primary @ 40%)`) with `bodyMedium` text and no horizontal inset, visually inconsistent with the filled `surfaceVariant` "Use to get:" and auto-points sections directly below it on the same screen. Fixed the group picker's Card treatment, padding, and font size to match, then audited whether the fix itself introduced new duplication — it had: the same filled-`Card`-plus-`surfaceVariant` recipe was already copy-pasted across 6 call sites app-wide, and the group picker's radio-row markup reimplemented an existing shared component (`RadioRow`) inline instead of using it.

#### Duplication ✅ — found and fixed
Extracted `EarnItSectionCard` (`EarnItButtons.kt`) — fixed `surfaceVariant` fill, overridable `shape`/`elevation` — and migrated all 6 pre-existing inline `Card(... surfaceVariant ...)` sites onto it: the group picker, auto-points block, and "Use to get:" reward rows in `TaskEditScreen.kt`; the task row in `RewardEditScreen.kt`; both cards in `HistoryScreen.kt`; the template card in `TaskLibraryScreen.kt` (kept its own `elevation = 2.dp` override). Separately, the group picker's existing-group row reimplemented `RadioRow` (already used once, in `TasksScreen.kt`'s multi-reward log dialog) rather than calling it — extended `RadioRow` with optional `textStyle`/`contentPadding`/`labelSpacing` params, all defaulting to its exact prior behavior (verified `TasksScreen.kt`'s call site needed no changes), and migrated the group picker's existing-group row onto it.

#### Decoupling ✅ (n/a)
Pure UI/styling change — no ViewModel, Repository, or Dao touched.

#### Complexity & Pattern Health ✅ (checked)
Neither extraction is single-caller: `EarnItSectionCard` has 6 call sites, `RadioRow` has 2 (`TasksScreen.kt`, `TaskEditScreen.kt`). The group picker's "+ New group" row (radio + editable text field + clear button) was left as raw `RadioButton` rather than forced into `RadioRow`'s label-only shape — a genuine variant, not a missed absorption.

#### Dead Code & Hygiene ✅
Removed now-unused `Card`/`CardDefaults` imports from `TaskEditScreen.kt`, `RewardEditScreen.kt`, `HistoryScreen.kt` (`CardDefaults` still needed in `TaskLibraryScreen.kt` for its elevation override, so only `Card` was dropped there). `./gradlew ktlintCheck` clean. `git status` clean apart from the intended files.

#### Naming Consistency ✅
`EarnItSectionCard` follows the `EarnItPrimaryButton`/`EarnItOutlinedButton` naming precedent already in `EarnItButtons.kt`.

#### Hardcoded Values ✅ (n/a)
No new hardcoded colors — `EarnItSectionCard` centralizes the `surfaceVariant` fill itself. Shape radii (12dp/16dp) are passed per call site matching each one's prior value, consistent with the app's existing convention of inlining `RoundedCornerShape(...)` rather than naming shape constants.

#### Accessibility ✅ (checked)
`RadioButton` touch targets unchanged (component enforces its own 48dp minimum regardless of surrounding padding); all `IconButton` content descriptions unchanged.

#### Deprecated APIs ✅
None touched.

#### Spec Review ✅
`EARNIT_SPEC.md`'s Task Edit line described the group picker as "a bordered radio-button list of existing groups" — stale after this fix. Updated to describe the filled-card styling, matching the auto-points and "Use to get:" sections.

#### Tests ✅ (0 new files, full suite re-verified)
- No new test files — pure styling/extraction change with no new logic paths.
- `./gradlew ktlintCheck`, `test`, `assembleDebug` all pass sequentially per `CLAUDE.md`.
- Ran the full `connectedDebugAndroidTest` suite (107 tests) on a connected emulator to confirm the `EarnItSectionCard`/`RadioRow` migration didn't regress any of the 4 touched screens, including `TasksScreen.kt`'s pre-existing `RadioRow` call site. One failure surfaced (`SettingsScreenUiTest.nickname_clearedField_greetingShowsNoAddress`, `ComposeNotIdleException` global-idle-timeout) in a file this branch never touched; re-ran that class alone and all 12 passed — confirmed emulator flake, not a regression.
- Found but deferred: `LogForRewardDialog`'s multi-reward branch (`RadioRow`'s other call site) has no instrumented coverage — the only existing test touching that dialog (`SharedDialogsCancelUiTest.logForRewardDialog_cancel_noLogRecorded`) links its task to a single reward, which takes the `else` branch and never renders `RadioRow`. Pre-existing gap, not introduced by this branch; left as a candidate for a future QA audit pass rather than expanding this pass's scope.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required.
- `TESTING.md`: no changes needed — no test added, removed, or renamed, so no counts drifted.

---

### Pass 54 — `chore/ci-test-report` branch

CI had no visibility into test counts — a PR's Checks tab showed only a single green/red step for `./gradlew test` or `connectedDebugAndroidTest`, with pass/fail/skip counts buried in logs. Mirrored `hodit`'s reference setup: added a `mikepenz/action-junit-report@v4` step, `if: always()`, right after each existing test-execution step in `ci.yml` (unit tests) and `instrumented-tests.yml` (per shard), each publishing a named Check Run and job summary from the JUnit XML already being produced. Added the required `permissions: checks: write` / `pull-requests: write` block to both workflows. Config-only change — no Kotlin, Compose, or app code touched.

#### Duplication ✅ (checked)
The two `action-junit-report` steps are near-identical (differ only in `check_name`/`report_paths`). Checked whether to extract a shared composite action: hodit's own reference doesn't extract it either (inlined in both its workflows), and `instrumented-tests.yml` already inlines its `android-emulator-runner` config twice in the same file — inlining two call sites matches existing project convention, not a missed extraction.

#### Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility ✅ (n/a)
No Kotlin, Compose, ViewModel, Repository, or Dao touched; no new files added.

#### Dead Code & Hygiene ✅
`git status`/`git diff --stat` confirm exactly the 4 intended files changed (30 insertions, 2 deletions), nothing stray.

#### Deprecated APIs ✅ (checked)
`mikepenz/action-junit-report@v4` matches the version hodit currently pins in both of its workflows — not stale relative to the reference being mirrored.

#### Spec Review ✅ (checked, no changes needed)
Grepped `EARNIT_SPEC.md` for `CI|Workflow|GitHub Actions|test-results|action-junit` — no hits. The spec doesn't document CI/test infrastructure, so there's no contract to reconcile (same precedent as former Pass 51).

#### Tests ✅ (0 new test cases, docs updated)
- No test files added, removed, or renamed — this pass only adds reporting visibility around existing test execution.
- No `AppModule`/`TestAppModule`/`@Inject` changes, so `assembleDebugAndroidTest` wasn't required.
- `./gradlew ktlintCheck` passes (doesn't lint YAML, but is the project's standard pre-commit check).
- `TESTING.md`: checked the Cadence table and Deferrals section directly, not just the one line touched — the Cadence table already reflected both workflows running on every push/PR (unchanged by this pass, which adds visibility, not a trigger change); Deferrals' five entries are all unrelated coverage gaps, none tied to CI reporting. Only the CI-behavior sentence needed updating, now done.
- `DEV_PLAYBOOK.md`: Post-launch checklist item split — the artifacts/Check-Run portion this pass closes was struck out entirely (not left checked) per the "checklist should only ever contain open work" rule; the README test-count badge portion stays open, deferred (needs a dynamic-badge/gist mechanism, out of scope here).

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
