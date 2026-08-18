# EarnIt — Cleanup Log

This log keeps only the **3 most recent** cleanup passes. Each pass follows the checklist in [CLEANUP_RULES.md](CLEANUP_RULES.md) and covers: duplication, decoupling, complexity, dead code, naming consistency, hardcoded values, accessibility, deprecated APIs, spec alignment, and test coverage. Passes are triggered after any significant feature work or refactor.

Full history isn't lost — every past pass is tracked in git history and in merged PRs on GitHub. This file is for the latest details only, not a permanent archive.

> **How to add a new entry:** Copy the checklist from [CLEANUP_RULES.md](CLEANUP_RULES.md), paste a new `### Pass N — description` section at the bottom of this file, and tick off what you found and fixed. If this pushes the log past 3 entries, delete the oldest one(s).

---

### Pass 69 — `chore/release-v1.3.0-prep` branch (doc sync ahead of the v1.3.0 release cut)

User asked to confirm README.md and EARNIT_SPEC.md were current before cutting the v1.3.0 release. Pass 66–68 had each done a spec review, so EARNIT_SPEC.md's feature coverage was accurate, but README.md was never touched by any of the three preceding feature branches and had drifted; both docs also carried one shared stale claim and a stale test-count line.

Most checklist sections are n/a — this is a documentation-only pass, no source changes.

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility / Deprecated APIs / Dev Seed Data — n/a
No source code touched this pass.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only the intended doc + `app/build.gradle.kts` changes.

#### Spec Review ✅ — found and fixed
- `README.md`: missing the first-launch onboarding tutorial from the feature list entirely; the backup bullet didn't mention it's opt-out; the intro line and Tech Stack table both claimed an absolute "local only, no cloud sync" that contradicts the Auto Backup opt-out shipped in Pass 67 (the same class of inaccuracy Pass 67 already fixed in the in-app About copy, just missed in README). Fixed all three.
- `EARNIT_SPEC.md`: the Overview and Stack table carried the same absolute "local only, no cloud" wording as README, inconsistent with §7's own accurate description of the opt-out backup. Fixed both to match §7, with a cross-reference link from the Overview.
- `EARNIT_SPEC.md`'s test-summary line (§ Tests) was stale on two axes: test counts (203/119/85 vs. actual 206/120/85 per `TESTING.md`'s current headers) and file counts (claimed 29/41, actual 28/32 — verified against both the filesystem and `TESTING.md`'s own table row counts). Corrected to 206 unit tests / 28 files, 120 instrumented / 32 files, 85 Compose UI tests.
- Did not touch `TESTING.md`'s test pyramid (`Unit — 205 tests` / `UI — 85 tests` / `Integration — 34 tests`, which sums to 119 against the section header's 120) — that's a pre-existing internal inconsistency predating this branch, out of scope for a doc-sync pass; flagging here rather than silently leaving it for the next person to rediscover.

#### Tests — n/a
No test files changed.

#### Release
Version bump for the v1.3.0 cut: `app/build.gradle.kts` `versionCode` 3→4, `versionName` "1.2.0"→"1.3.0", per `DEV_PLAYBOOK.md`'s release process.

---

### Pass 70 — `test/settings-persistence-and-assertions` branch

QA audit Issues 6 and 7: two `SettingsScreenUiTest` `...persistsAfterRecreate` tests only re-read the DataStore-backed repository after `activityRule.scenario.recreate()`, which was already written before the recreate — they'd pass identically with the `recreate()` line deleted. `useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting` only asserted the *old* nickname was gone, never that a random one actually appeared, and `mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable` matched text anywhere on screen instead of scoping to the dialog. While fixing these, found that the audit's own cited counter-example, `SettingsUiTest.colorScheme_selectionPersistsAfterRecreate`, had the identical repo-only bug — not on the backlog list, fixed anyway for consistency, confirmed with the user first since it needed a small production change (see Accessibility below).

#### Duplication ✅ (checked, n/a)
The "re-navigate to Settings and assert rendered state" snippet now appears in three tests (colorScheme, mascot, cloud backup); each is 1–3 lines with a different target node, not worth extracting into a shared helper at this size.

#### Decoupling ✅ (checked, n/a)
Test-only changes plus one one-line production modifier swap in `ThemeChip` (`.clickable` → `.selectable`) — no logic moved between layers.

#### Complexity & Pattern Health ✅ (checked)
`Modifier.selectable(selected =, onClick =)` is the standard Compose replacement for a manually-tracked `selected` boolean plus `.clickable` — not a custom reimplementation of M3 selection behavior. `ThemeChip` is unchanged in size and structure.

#### Dead Code & Hygiene ✅ (checked)
`ktlintCheck` clean. Confirmed `clickable` (the import) is still used elsewhere in `SettingsScreen.kt` before leaving it in place. `git status` shows exactly the 5 intended files changed.

#### Naming Consistency ✅ (checked, n/a)

#### Hardcoded Values ✅ (checked, n/a)

#### Accessibility ✅ — found and fixed
`ThemeChip`'s selected state was previously conveyed only through border color/width and font weight — invisible to the semantics tree, so TalkBack couldn't announce which color scheme was selected. Exposing it via `Modifier.selectable`'s standard `selected` semantics fixes that alongside making the state testable; confirmed with the user before making the production change since it was outside the branch's original test-only scope.

#### Deprecated APIs ✅ (checked, n/a)

#### Spec Review ✅ (checked, n/a)
No behavior change — `EARNIT_SPEC.md` doesn't document test/UI semantics at this level of detail.

#### Tests ✅ — this branch's entire purpose
Rewrote `selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate` and `cloudBackupToggle_defaultsOn_turnedOff_persistsAfterRecreate` to assert against rendered UI after recreate (Settings mascot row text; cloud-backup Switch `assertIsOff()`), keeping the repository check as a secondary assertion. Fixed `colorScheme_selectionPersistsAfterRecreate` the same way via the new `selected` semantics. Made `useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting` assert a random-nickname-shaped greeting actually renders (`SemanticsMatcher` on text starting with `"Earn It, "` and not equal to the old value) and dropped its one redundant `assertEquals`. Scoped `mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable`'s Pugsly/Tabby lookups to the dialog via `hasAnyAncestor(isDialog())` (mirroring `CancelDismissAssertions`), asserted both carry a click action, and added a check that Panda's name is hidden.
Load-bearing check: temporarily reverted the `ThemeChip` `.selectable(...)` change and re-ran `SettingsUiTest` — `colorScheme_selectionPersistsAfterRecreate` failed with a real `AssertionError` on `assertIsSelected()`, confirming the new assertion isn't vacuous; reverted the revert and re-confirmed green.
Ran `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.earnit.app.SettingsScreenUiTest,com.earnit.app.SettingsUiTest` on the connected emulator (API 36): 16/16 passed, 0 skipped, 0 failed.
No `AppModule`/`TestAppModule` change or new `@Inject` site — ran `./gradlew assembleDebugAndroidTest` anyway per the Hilt-graph-sanity-check convention; passed.
`checkInstrumentedTestTags`: both files already carry their required layer tags; no new test classes added, no tag changes needed.
`TESTING.md`: `SettingsUiTest` and `SettingsScreenUiTest` row descriptions updated to state the recreate tests now confirm rendered UI, the nickname test asserts a positive outcome, and the mascot-picker checks are dialog-scoped. No aggregate counts changed — same number of tests, no new files.
`./gradlew ktlintCheck`, `test` (204/204), `assembleDebugAndroidTest` all pass sequentially.

#### Dev Seed Data ✅ (checked, n/a)
No `TestDataSeeder` changes — this branch touches only DataStore-backed settings already covered by existing seed data.

---

### Pass 71 — `chore/qa-audit-doc-fixes` branch

QA audit Issues 8, 9, 10, and 11: dangling `CLEANUP_BACKLOG.md` doc references and stale bug-history comments in `RewardEditScreenUiTest.kt`, `CleanUpScreenUiTest.kt`'s KDoc not covering its fifth (confirm) test, `TestStateReset.kt`'s KDoc describing removed cold-start navigation, a dead `TaskEntity.repeatable` field documented in the spec as functional, drifted test-count figures, and a stale "Widget colors hardcoded warm-gold" Known Limitation. While re-checking README beyond the audit's original scope, also found the opening tagline still carried the same unqualified "local only" claim Pass 69 already fixed elsewhere in the same file.

#### Duplication / Decoupling / Complexity & Pattern Health / Naming Consistency / Hardcoded Values / Accessibility / Deprecated APIs / Dev Seed Data — n/a
Doc/comment fixes plus one one-line Kotlin default-value change; no structural code touched.

#### Dead Code & Hygiene ✅ (checked)
`git status` shows only the 10 intended files changed. Removed both `CLEANUP_BACKLOG.md` references (a file that doesn't exist in `docs/`) and rewrote the comments around them to describe current, working behaviour instead of a fixed historical bug.

#### Spec Review ✅ — found and fixed
`EARNIT_SPEC.md` §1's Task Fields table and §10's Screen Map both documented `TaskEntity.repeatable` as functional (a "repeatable toggle" on Task Edit); confirmed dead in production — `TaskEditScreen.kt` only round-trips the field, nothing reads it, and repeatability is governed entirely by `RewardTaskCrossRef.isRepeatable` instead. Removed both. Kept the column itself rather than deleting it — the app is at Room schema v1 with a hard "every version bump ships a real migration" rule and no existing migration to pattern off, not worth it for a field with zero behavioural payoff. Separately, `RewardTaskCrossRef`'s own constructor default (`isRepeatable = false`) contradicted the documented default (`true`, matching `addTaskToReward`'s own default) — aligned it; confirmed low risk first, since only `GatekeeperTest`/`JsonExportTest` relied on the old default and neither asserts on the value itself.

#### Tests ✅ — counts corrected, no test behaviour changed
The audit's own baseline (204 unit / 121 instrumented) and even `TESTING.md`'s live headers (207/120) were both already stale — three since-merged branches had added tests without updating every count. Recomputed against a fresh `@Test` grep: 220 unit tests / 122 instrumented (87 UI-tagged, 35 Repository/Utility-tagged). Updated `TESTING.md`'s pyramid, headers, and two per-file rows (`FieldValidationTest` 14→20, `RewardProgressTest` 21→28), `EARNIT_SPEC.md` §9, `README.md`'s badge and prose, and `MANUAL_TEST_PLAN.md`'s `ExportImportTest` count (5→11). `./gradlew ktlintCheck`, `test` (220/220), and `assembleDebugAndroidTest` (run specifically because `Entities.kt` changed) all pass sequentially.
