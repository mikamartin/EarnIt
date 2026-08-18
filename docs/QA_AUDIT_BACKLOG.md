# EarnIt — QA Audit Backlog

Tracks findings from the most recent QA audit (see [QA_AUDIT_RULES.md](QA_AUDIT_RULES.md) for
the procedure). When a listed follow-up branch lands, don't delete its entry — condense it:
shrink its Issues Found entry to one sentence stating what it was and that it's fixed (keep the
number, so it stays findable), and replace its Work Item's Steps with a short, dry summary of
what was actually done, marked `(done)`.

This pass ran in **default mode**: findings are recorded here only, and no fix — including the
doc-hygiene ones — was applied on the audit branch. Every item below is proposed work awaiting
a go-ahead.

Baseline for the pass: `./gradlew ktlintCheck` clean, `./gradlew test` 204/204 passing,
`./gradlew :app:minifyReleaseWithR8` succeeds. Actual test counts measured at audit time:
**204 unit** tests across 28 files, **121 instrumented** tests across 32 files (34 Repository /
Utility, 87 UI).

---

## What's Working

- **CI test-execution integrity is sound.** Every one of the 32 test-bearing `androidTest`
  classes carries a required layer tag, and the two shards (`annotation=UiTest` and
  `notAnnotation=UiTest`) are exact complements, so no class can fall through the gap. Unit tests
  run via a bare `./gradlew test` with no class list, so a new unit test file cannot be orphaned
  either. No test class exists that never runs in CI.
- **Mutation resistance on core mechanics is strong.** 10 of 13 mutations were caught, including
  every boundary on the point formula, the claim gate, the reward-count cap, import dedup
  case-folding, the nudge thresholds, mascot unlock thresholds, the character-cap transform, and
  the widget's hint boundary and button priority. Boundary conditions are genuinely pinned, not
  just exercised.
- **Spec alignment on the headline contracts is accurate.** The auto-point formula (base,
  dimension-5 bonus, min 1 / max 30), the two-condition claim gate, per-reward point isolation,
  start-over vs. archive claim semantics, and import replace-vs-merge conflict strategies all
  match `EARNIT_SPEC.md` exactly, and the tests assert the spec's stated values rather than
  merely re-deriving the code's.
- **Field-transform extraction is complete.** Every capped, digit-filtered, or
  sibling-resetting text field in the app routes through `FieldValidation.kt`'s pure functions —
  no inline `onValueChange` logic was found that still needs extracting. `DragReorder`,
  `PugslyGesture`, `WidgetActionButton`, and `OnboardingLogic` follow the same pattern.
- **Shared test helpers are real and used.** `RoomIntegrationBase`, `RepositoryTestBase`,
  `ViewModelTestBase`, `TestStateReset`, `UiTestActions`, and `CancelDismissAssertions` are
  imported rather than redefined per file, and `CleanUpScreenUiTest`'s four cancel tests
  collapse into one parameterised private helper.
- **Enum persistence survives R8.** `MascotId` / `AppColorScheme` are stored in DataStore by
  `.name`; the release dex was checked and the constant name strings (`PUGSLY`, `OCEAN_BLUE`)
  survive shrinking, so a user's theme and mascot selection cannot be lost to obfuscation.
- **No dangling `Pass N` cross-references** anywhere in the repo (docs, source comments, CI
  config) — every reference is inside `CLEANUP_LOG.md`'s retained 3-pass window, and the log's
  entries are in the oldest-first order its header describes.

---

## Issues Found

### 1. `RewardTaskCrossRef` was obfuscated by R8 while Moshi serialised it reflectively, breaking release-build JSON export/import of reward↔task links — fixed on `fix/moshi-crossref-keep-rule`.

### 2. Wrong-file import could silently wipe all user data in Replace mode — fixed on `fix/import-schema-validation`.

### 3. `ClearCascadeTest`'s two cross-ref assertions were vacuous, so the FK cascade was never actually verified — fixed on `test/fk-cascade-and-cleanup-assertions`.

### 4. `showsProgressNumbers`' `!canClaim` term is dead code, and its test passes for the wrong reason

`RewardProgress.showsProgressNumbers` is `!canClaim && totalPoints < reward.cost`. Since `canClaim`
requires `totalPoints >= reward.cost`, `totalPoints < cost` already implies `!canClaim` — the first
term can never change the result. Deleting it entirely leaves all 21 `RewardProgressTest` tests
green (verified). No test can catch this, by construction.

The consequence is that `showsProgressNumbers false when points meet cost and mandatory task
unlogged` — the test that reads as pinning the mandatory-gate interaction — is actually only
pinning `totalPoints >= cost`. The mandatory-task half of that scenario is unasserted.

### 5. The import schema-key check's exactness was untested, which is how Issue 2 stayed invisible — fixed on `fix/import-schema-validation`.

### 6. Two "persistsAfterRecreate" tests didn't actually test persistence across recreate — fixed on `test/settings-persistence-and-assertions`.

### 7. `useRandomNickname` and mascot-picker tests under-asserted their own claims — fixed on `test/settings-persistence-and-assertions`.

### 8. Stale comments and a dangling doc reference in `RewardEditScreenUiTest`

Two references to `docs/CLEANUP_BACKLOG.md` (lines 33 and 200) — a file that does not exist in
`docs/`. Both describe `sequentialCreateNewTasks_onUnsavedReward_bothStayIncluded` as pinning a
"known pre-existing bug" where the first task's inclusion is silently dropped. That bug is fixed:
the test name, its assertions, and `TESTING.md`'s row all describe the working behaviour. The class
KDoc still narrates the broken state, against `CLAUDE.md`'s rule that comments describe current
behaviour rather than history.

Two more in the same class: `CleanUpScreenUiTest`'s KDoc says "each test seeds a task, reward, and
log, cancels the dialog, and confirms … that nothing was actually cleared", which no longer
describes the fifth test (the Wipe Everything confirm path added in Pass 68). And
`TestStateReset.kt`'s KDoc justifies `markOnboardingSeen()` with "EarnItApp auto-navigates to
Create Reward whenever it isn't" — `EarnItApp.kt`'s own comment states that cold-start navigation
was deliberately removed and the tutorial is now hosted by `HomeScreen`.

### 9. `TaskEntity.repeatable` is dead data that the spec documents as functional

`EARNIT_SPEC.md` §1's Task Fields table lists `Repeatable | Boolean | Default true. If true, can
be logged multiple times.` Nothing reads it: the only reference in the whole app is
`TaskEditScreen.kt:321`, which writes it straight back to itself
(`repeatable = existing?.repeatable ?: true`). Repeatability is governed entirely by
`RewardTaskCrossRef.isRepeatable`, which is what `logCompletion` and `RewardProgress.loggableTasks`
consult. §10's Screen Map also lists a "repeatable toggle" on Task Edit; that toggle only exists
per reward-link on Reward Edit.

Related trap: `RewardTaskCrossRef`'s own constructor default is `isRepeatable = false`, the
opposite of the `isRepeatable = true` default §3 documents for a new link (and that
`addTaskToReward` implements). All four current construction sites pass both flags explicitly, so
nothing is broken today — but a future call site that omits the argument silently gets the
inverse of the documented behaviour.

### 10. Test-count figures are wrong in three places

`CLEANUP_RULES.md` sets the convention: aggregates rounded to the nearest 5 or 10, per-file counts
exact. Measured against that, most figures are fine (pyramid `Unit — 205` / `UI — 85`, headers and
cadence `120`, `Integration — 34`). Three are not:

| Location | States | Actual | Problem |
|---|---|---|---|
| `TESTING.md` "Unit Tests — `app/src/test/` (206 tests)" | 206 | 204 | Overstates, and 206 is not a rounded figure |
| `EARNIT_SPEC.md` §9 Summary | 206 unit tests | 204 | Same figure, copied from the header above during Pass 69 |
| `README.md` badge + line 110 | "206+" unit tests | 204 | `+` makes it an overclaim in a public-facing badge |
| `MANUAL_TEST_PLAN.md` Export/Import rationale | `ExportImportTest.kt` (5 tests) | 10 | Per-file count, must be exact |

Everything else in `TESTING.md`'s two per-file tables is exact: all 28 unit rows and all 32
instrumented rows were recomputed and match their files' `@Test` counts, summing to 204 and 121
respectively.

### 11. `DEV_PLAYBOOK.md` lists a stale "known limitation" that contradicts the spec and the tests

"Widget colors hardcoded warm-gold — Glance limitation." is the first entry under *Known
Limitations* — a section whose own preamble says it holds "permanent, accepted constraints — not
open work". It is no longer true: `EARNIT_SPEC.md` §5's Widget Theme section describes colours
resolving per scheme at draw time from `ColorSchemes.lightColors`/`darkColors`, and
`WidgetColorsTest` (4 tests) asserts exactly that across all three schemes plus dark mode.

### 12. `checkInstrumentedTestTags` scanned whole-file text rather than per class, so a missing/duplicate/comment-only tag on one class in a multi-class file could pass — fixed on `chore/ci-release-build-gate`.

### 13. Derived hint state is duplicated across three surfaces with no shared definition

`showMandatoryHint` (`!canClaim && totalPoints >= cost`) is written out independently in
`HomeScreen.kt:581`, `RewardDetailScreen.kt:125`, and `EarnItWidget.kt:365`.
`showAllTasksLoggedHint` is written out in `HomeScreen.kt:582` and `RewardDetailScreen.kt:126` as
`!canClaim && allTasks.isNotEmpty() && loggableTasks.isEmpty()`, while `EarnItWidget.kt:366`
expresses the same predicate a fourth way as `actionButton == WidgetActionButton.LOG_DISABLED`.
All are currently equivalent; none of the three UI-layer copies has a unit test (only the widget's
formulation does, via `WidgetActionButtonTest`/`WidgetContentTest`). This is the same drift shape
`DragReorder` was extracted to fix — they belong on `RewardProgress` next to
`showsProgressNumbers`.

Second instance: the duplicate-name check is reimplemented in `TaskEditScreen.kt:157` and
`RewardEditScreen.kt:197` with the same four-part shape
(`!pendingSaveNav && name.isNotBlank() && list.any { trim().equals(…, ignoreCase = true) && id != self }`).
It is pure — no Compose or Android dependency — and currently exercised only through the two
full-`MainActivity` tests in `DuplicateNameUiTest`.

### 14. The `RepositoryBehaviourTest` reward-cap mutation was caught by an unstubbed mock, not by its assertion

Mutating the cap check from `>=` to `>` fails
`copyRewardFromEntry blocks the copy when the active reward count is already at the max` with
`io.mockk.MockKException at RepositoryBehaviourTest.kt:197` — the strict `rewardDao` mock has no
stub for the `getReward` call the mutated code then reaches. The
`assertEquals(MAX_REWARDS_REACHED, outcome)` never runs.

The assertion *is* a genuine backstop (with the mock chain completed the outcome would be `ADDED`
and the assertion would fire), so this is a diagnostic-quality problem rather than a coverage hole:
the failure points at the `runBlocking` line instead of the behaviour, which makes the real cause
slow to find. Recording it here per the audit rules' weak-pass category.

### 15. Smaller test-quality notes

- **`clearAllLogs_removesActiveAndArchivedLogs`** now also asserts history entries are deleted —
  fixed on `test/fk-cascade-and-cleanup-assertions`.
- **`clearAllDialog_confirm_wipesDataAndResetsSettingsExceptBackupChoice`** now also asserts
  `onboardingSeen` resets — fixed on `test/fk-cascade-and-cleanup-assertions`.
- **`assertCancelClearsNothing`**'s Clear Logs cancel path now also asserts history survives —
  fixed on `test/fk-cascade-and-cleanup-assertions`.
- **The `logCompletion` archived-reward guard has no JVM-tier coverage.** Deleting
  `if (reward.isArchived) return@withTransaction` leaves the *entire* 204-test unit suite green.
  This is by design — `LogAgainstArchivedRewardTest` covers it at the instrumented Repository
  tier — but it means the guard is only protected on the emulator job, noted here so the
  dependency is explicit rather than incidental.

### 16. CI never built the minified release variant, so a shrinker-only defect like Issue 1 could ship undetected until a tag push — fixed on `chore/ci-release-build-gate`.

---

## Mutation Check Results

13 mutations across 11 test files and 8 source files. Every mutation was reverted immediately;
`git status` was verified clean before writing this file.

| # | Test file(s) | Tier | Source mutated | Mutation | Result |
|---|---|---|---|---|---|
| 1 | `PointFormulaTest` | Pure | `Entities.kt` `computeAutoPoints` | bonus threshold `== 5` → `== 4` | ✅ Caught — 6/10 failed, `AssertionError` |
| 2 | `GatekeeperTest` | Pure | `Entities.kt` `canClaim` | `totalPoints >= cost` → `>` | ✅ Caught — 2/7 failed (exact-equal and zero-cost cases) |
| 3 | `RewardProgressTest` | Pure | `Entities.kt` `showsProgressNumbers` | drop `!canClaim &&` | ❌ **Missed** — all 21 pass. Traced to a redundant condition, not a weak assertion: the term is logically unreachable, so no test could catch it. See Issue 4 |
| 4 | `RepositoryBehaviourTest` | MockK repo | `EarnItRepository.copyRewardFromEntry` | cap `>= maxActiveRewards` → `>` | ⚠️ **Weak pass** — failed via `io.mockk.MockKException` (unstubbed `getReward`), not the `assertEquals`. See Issue 14 |
| 5 | `ImportDedupTest` | MockK repo | `EarnItRepository.importTemplate` | drop `.lowercase()` on existing names | ✅ Caught — 4/7 failed, `AssertionError` |
| 6 | `JsonImportValidationTest`, `JsonExportTest` | Pure | `JsonExport.fromJson` | `contains("\"$key\"")` → `contains(key)` | ❌ **Missed** — all 13 pass. Genuine coverage gap on the schema gate. See Issue 5 |
| 7a | `WidgetActionButtonTest` | Pure | `WidgetActionButton.kt` | reorder `ADD_TASK` above `LOG` | ⚪ Invalid (equivalent mutant) — `loggableTasks` is a subset of `allTasks`, so the two branches are mutually exclusive and the reorder is a semantic no-op. Replaced by 7b |
| 7b | `WidgetActionButtonTest` | Pure | `WidgetActionButton.kt` | demote `CLAIM` below `LOG` | ✅ Caught — `canClaim true returns CLAIM even if a repeatable task is still loggable`, `AssertionError` |
| 8 | `NudgeDeciderTest` | Pure | `NudgeDecider.FIRST_THRESHOLD_HOURS` | `48` → `47` | ✅ Caught — `idle under 48h returns NoOp`, `AssertionError` |
| 9 | `FieldValidationTest` | Pure | `FieldValidation.acceptWithinLimit` | `length <= max` → `<` | ✅ Caught — 2/14 failed (at-max and same-length-replacement cases) |
| 10 | `WidgetContentTest` | Robolectric + glance-testing | `EarnItWidget.showMandatoryHint` | `totalPoints >= cost` → `>` | ✅ Caught — `standardContent_mandatoryTaskUnloggedButPointsMet_showsHint`, `AssertionError` |
| 11 | `NudgeWorkerTest` | Robolectric + work-testing | `NudgeWorker.doWork` | `getActiveRewardCount() > 0` → `>= 0` | ✅ Caught — `does not notify when there is no active reward`, `AssertionError` |
| 12 | `MascotUnlockTest` | Pure | `Mascots.computeNewlyUnlocked` | `PointsReached` `>=` → `>` | ✅ Caught — 2/8 failed, `AssertionError` |
| 13 | `LogAttributionTest`, then the full 204-test unit suite | MockK repo | `EarnItRepository.logCompletion` | remove `if (reward.isArchived) return` | ❌ **Missed at the JVM tier** — whole unit suite green. Covered instead by the instrumented `LogAgainstArchivedRewardTest`. See Issue 15 |

**Tally:** 10 caught cleanly · 1 weak pass · 2 genuine/structural misses · 1 invalid mutant.

No new pure function was extracted during this pass, so the rules' "spot-check newly extracted
functions too" clause does not apply.

---

## Spec Cross-Reference Notes

Checked `EARNIT_SPEC.md`'s documented core mechanics against the corresponding assertions.

**Confirmed aligned:**

- **§1 Auto-Point Formula** — `TaskEntity.computeAutoPoints` implements
  `((t+1)(d+1)(p+1) + 7) / 8 + (3 if max == 5)` exactly as documented, including the integer-ceiling
  form. `PointFormulaTest` asserts the spec's stated range endpoints (min 1 at all-1s, max 30 at
  all-5s) and the dimension-5 bonus boundary in both directions, per dimension.
- **§3 Gatekeeper Logic** — the spec's two numbered conditions map one-to-one onto
  `RewardProgress.canClaim`. `GatekeeperTest` and `RewardProgressTest` between them assert the
  points boundary (exact / one-below / zero-cost), the mandatory gate in isolation, and the
  spec-critical combination of a zero-cost reward with an unlogged mandatory task.
- **§2 Per-Reward Point Pool** — `observeUiState` scopes `activeLogs` per `rewardId` and
  `claimReward` archives every active log for the reward with no partial attribution, matching
  "excess logs beyond the reward's cost are archived in full".
- **§3 Claiming Flow** — `startOver = true` skips the `isArchived` update and still writes the
  history entry; `ClaimRewardStartOverTest` and `StartOverTest` assert both halves plus the
  balance reset.
- **§7 Import modes** — Replace → `clearAllTables()` then plain inserts; Merge → the `*Ignore`
  DAO variants (`OnConflictStrategy.IGNORE`). Matches the spec table exactly.
- **§8a Inactivity Nudges** — the 48h/96h thresholds, the two-nudge cap, both guardrails, and the
  anchor-based streak reset all match `NudgeDecider`, and `NudgeDeciderTest` asserts each at its
  boundary.
- **§6 App Settings** — every row's default matches `SettingsRepository`, and
  `resetForWipeEverything()` implements the documented "resets everything except Cloud Backup
  Enabled" exception literally (clear, then restore that one key).

**Mismatches flagged (detail in Issues Found):**

- **§7 validation step 3** overstated what the schema check caught — Issue 2 (fixed; wording corrected).
- **§1 Task Fields `Repeatable`** documents a field with no effect; **§10** lists a Task Edit
  repeatable toggle that doesn't exist — Issue 9.
- **§5 Widget Theme** is correct, but `DEV_PLAYBOOK.md`'s Known Limitations contradicts it —
  Issue 11.
- **§9 Tests summary** count is stale — Issue 10.

**Noted, not a mismatch:**

- **§3's task ordering** ("mandatory tasks first (A→Z), then optional (A→Z)") is implemented in the
  view, at `RewardDetailScreen.kt:612`, not in the model. `RewardProgress.allTasks` is
  `mandatoryTasks + optionalTasks` in cross-ref insertion order, and that is what `HomeScreen` and
  the widget consume. The spec scopes its claim to the Reward Detail list, so this is accurate as
  written — but `RewardProgressTest`'s `allTasks returns mandatory followed by optional` pins
  insertion order, not the alphabetical order a reader of §3 might expect the model to guarantee.
- **`RewardProgress` has no spec anchor of its own** for `progressFraction`'s zero-cost branch
  (`cost <= 0` → `1f`). It's tested and sensible; the spec just never states it.

---

## Work, Grouped by Branch

Seven proposed branches, ordered by severity. Five have landed; the rest have not been started.

### `fix/moshi-crossref-keep-rule` — Issue 1 (done)

**Steps (done):** Pinned the symptom on an installed signed release build first (import of a
correctly-keyed backup failed with the generic "Import failed ✗" fallback — an exception type
`JsonExport.fromJson`'s catch blocks didn't even recognize). Fixed the root cause: added
`@JsonClass(generateAdapter = true)` to all five export element types, dropped
`KotlinJsonAdapterFactory` and the now-unused `moshi-kotlin` dependency. Confirmed via
`mapping.txt` that all five generated adapters and `RewardTaskCrossRef` itself are kept
unobfuscated, and re-ran the same on-device round trip to confirm both import and export now use
real key names. Added `MANUAL_TEST_PLAN.md`'s release-build step. No shipped backups exist yet
(pre-Play-Store-launch), so no import compatibility shim was added.

**Tests:** Added a `JsonExportTest` case pinning the exact top-level and per-entity JSON key names
for a fully populated `EarnItExport`.

### `fix/import-schema-validation` — Issues 2 and 5 (done)

**Steps (done):** Replaced the substring gate in `JsonExport.fromJson` with a two-stage parse: the
top level is first parsed into a `Map<String, Any?>` and required to contain all five expected keys
(`tasks`, `rewards`, `rewardTaskCrossRefs`, `completionLogs`, `historyEntries`), since
`JsonExport.toJson` always emits all five; only then is the typed `EarnItExport` parse attempted.
Genuine JSON syntax errors (caught during the generic parse) throw `ImportInvalidJsonException`;
anything syntactically valid but the wrong shape — missing keys, a non-object top level, or a
recognized key holding wrong-shaped elements — throws `ImportWrongSchemaException`, fixing the case
where a recognizably-EarnIt-shaped file with a wrong element type was mislabeled as invalid JSON.
`ImportWrongSchemaException`/`ImportInvalidJsonException` stayed the thrown types, so the existing
ViewModel mapping and UI strings were unaffected. Updated `EARNIT_SPEC.md` §7's validation list to
describe what the check actually guarantees.

**Tests (done):** Extended `JsonImportValidationTest` with the Issue 2 probe (a foreign object
reusing one EarnIt key) and a case pinning the wrong-element-shape distinction; updated the
single-key-present test to expect `ImportWrongSchemaException` instead of success. Added
`ExportImportTest.importReplace_withForeignKeyMatchingSchema_doesNotWipeExistingData`, mirroring
the existing `importReplace_withWrongSchema_doesNotWipeExistingData`. `./gradlew test`,
`ktlintCheck`, `assembleDebugAndroidTest`, and `:app:minifyReleaseWithR8` all pass; the full
`ExportImportTest` class (11/11) confirmed passing via `connectedDebugAndroidTest` on-device.

### `test/fk-cascade-and-cleanup-assertions` — Issues 3 and 15 (done)

**Steps (done):** Gave `ClearCascadeTest` direct cross-ref reads via `RoomIntegrationBase`'s
`database.rewardTaskCrossRefDao()` and replaced the vacuous `progress.allTasks`-based assertions in
`deleteTask_…`, `clearAllTasks_…`, and `deleteReward_…` with direct row-count checks. Added a
history-entry row-count assertion to `clearAllLogs_removesActiveAndArchivedLogs`
(`database.historyDao().getAllEntries()`). In `CleanUpScreenUiTest`, seeded a history entry ahead
of the Clear Logs cancel test specifically (via `claimReward(startOver = true)`) and asserted it
survives cancel, rather than adding an always-trivially-passing history check to the shared
`assertCancelClearsNothing` helper. Added an `onboardingSeen` assertion to
`clearAllDialog_confirm_wipesDataAndResetsSettingsExceptBackupChoice` — scoped to the
settings-level check only, not a UI-level assertion that the tutorial re-renders on Home (would
require modeling `viewModel.onboardingStep`'s state machine, judged out of scope for this "smaller
note"). Removed the "FK cascade delete behaviour" entry from `TESTING.md`'s Deferrals, and
corrected `DeleteCascadeTest.kt`'s header comment to point at the new `ClearCascadeTest` assertions
instead of the false `MANUAL_TEST_PLAN.md`/`TESTING.md` claim.

**Tests (done):** `./gradlew ktlintCheck` and `./gradlew test` (204/204) both pass.
`connectedDebugAndroidTest` pinned to `com.earnit.app.ClearCascadeTest` and
`com.earnit.app.CleanUpScreenUiTest` (10/10) confirmed passing on-device. As a sanity check that
the new assertions aren't themselves vacuous, temporarily set both `RewardTaskCrossRef` foreign
keys in `Entities.kt` to `ForeignKey.NO_ACTION` and re-ran `ClearCascadeTest`: it failed with a
real `SQLiteConstraintException`, confirming the assertions are load-bearing; the change was then
reverted and the suite re-confirmed green.

### `refactor/reward-progress-derived-state` — Issue 13

**Deliverable:** The mandatory-hint and all-tasks-logged predicates have one definition with
direct unit coverage; the duplicate-name check likewise.

**Steps:**
1. Add `showsMandatoryHint` and `showsAllTasksLoggedHint` to `RewardProgress` alongside
   `showsProgressNumbers`, and point `HomeScreen`, `RewardDetailScreen`, and `EarnItWidget` at
   them. Fold Issue 4's dead `!canClaim` term out of `showsProgressNumbers` in the same pass.
2. Extract the duplicate-name predicate into a pure function (`FieldValidation.kt` is the
   established home) taking the candidate name, the existing names, and the self-id, and call it
   from both `TaskEditScreen` and `RewardEditScreen`.

**Tests:** Add `RewardProgressTest` cases for both new properties at their boundaries, and
`FieldValidationTest` cases for the name check (case-insensitive match, whitespace-trimmed match,
self-id excluded, blank name). Give each new function the one-mutation spot check this pass's
section 2 applied to sampled files before considering it validated. `DuplicateNameUiTest` and
`RewardProgressBarUiTest`/`RewardAllTasksLoggedHintUiTest` stay as the wiring proof.

### `test/settings-persistence-and-assertions` — Issues 6 and 7 (done)

**Steps (done):** Rewrote `selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate` and
`cloudBackupToggle_defaultsOn_turnedOff_persistsAfterRecreate` to re-navigate and assert against
rendered UI after `recreate()` — the Settings mascot row's text and the cloud-backup Switch's
`assertIsOff()` state — rather than only the repository, which is kept as a secondary check.
While implementing this, found that the backlog's own cited counter-example,
`SettingsUiTest.colorScheme_selectionPersistsAfterRecreate`, had the identical repo-only bug and
fixed it too: exposed `ThemeChip`'s existing `selected` boolean through the semantics tree (swapped
`.clickable` for `.selectable(selected = selected, onClick = …)`), then asserted
`onNodeWithText("Ocean Blue").assertIsSelected()` after recreate. Made
`useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting` assert a random-nickname-shaped
greeting actually appears (`SemanticsMatcher` on text starting with `"Earn It, "` and not equal to
the old `"Earn It, Zorro!"`) instead of only the old name's absence, and dropped the redundant
`assertEquals` after the `waitUntil`. Scoped `mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable`'s
Pugsly/Tabby lookups to the dialog via `hasAnyAncestor(isDialog())` (mirroring
`CancelDismissAssertions`), asserted both carry a click action, and added a check that Panda's name
is hidden. Updated `TESTING.md`'s `SettingsUiTest` and `SettingsScreenUiTest` rows to match.

**Tests (done):** `./gradlew ktlintCheck`, `./gradlew test` (204/204), and
`./gradlew assembleDebugAndroidTest` all pass. `connectedDebugAndroidTest` pinned to
`com.earnit.app.SettingsScreenUiTest` and `com.earnit.app.SettingsUiTest` (16/16) confirmed passing
on-device. As a load-bearing check, temporarily reverted the `ThemeChip` `.selectable(...)` change
and re-ran `SettingsUiTest`: `colorScheme_selectionPersistsAfterRecreate` failed with a real
`AssertionError` on `assertIsSelected()`, confirming the new assertion isn't vacuous; the change
was then restored and the suite re-confirmed green.

### `chore/qa-audit-doc-fixes` — Issues 8, 9, 10, 11

**Deliverable:** Docs and comments describe current behaviour with correct figures. Doc-only
except for the dead `TaskEntity.repeatable` field.

**Steps:**
1. Fix the counts from Issue 10: `TESTING.md` unit header 206 → 205 (rounded) or 204,
   `EARNIT_SPEC.md` §9 to match, `README.md`'s badge and line 110, and `MANUAL_TEST_PLAN.md`'s
   `ExportImportTest` count 5 → 10.
2. Delete the two `CLEANUP_BACKLOG.md` references in `RewardEditScreenUiTest.kt` and rewrite both
   comments to describe the working behaviour. Refresh `CleanUpScreenUiTest`'s and
   `TestStateReset.kt`'s KDoc.
3. Remove the stale "Widget colors hardcoded warm-gold" entry from `DEV_PLAYBOOK.md`'s Known
   Limitations.
4. Fix `README.md`'s mermaid `DB[("SQLite\nlocal only")]` node, the one remaining absolute
   "local only" claim Pass 69 missed. Add `QA_AUDIT_BACKLOG.md` to the README documentation table.
5. Resolve Issue 9's dead field: either delete `TaskEntity.repeatable` (a schema change, so it
   needs a `Migration` per `DEV_PLAYBOOK.md` §4 — likely not worth it alone) or keep the column and
   correct the spec, removing the §1 row's behavioural claim and the §10 Task Edit "repeatable
   toggle". Also align `RewardTaskCrossRef`'s constructor default with the documented
   `isRepeatable = true`, or document why the entity default deliberately differs.

**Tests:** No behaviour change if the field is kept. If `TaskEntity.repeatable` is dropped, this
stops being doc-hygiene and needs its own branch with a migration and `./gradlew test` +
`assembleDebugAndroidTest`.

### `chore/ci-release-build-gate` — Issues 12 and 16 (done)

**Steps (done):** Added a `./gradlew :app:minifyReleaseWithR8` step to `ci.yml` — confirmed it
needs no keystore, so no signing workaround was required. Added `ktlintCheck` and `test` steps to
`release.yml` before the keystore secrets are touched, so a bad tag fails loudly without ever
decoding them. Rewrote `checkInstrumentedTestTags` in `app/build.gradle.kts` to strip comments,
walk each class's own annotation header and brace-matched body, and require exactly one layer tag
plus at least one optional tag per test-bearing class — kept the existing grouped failure-message
shape and added a new bucket for the "more than one layer tag" case.

**Tests (done):** Confirmed a clean pass against the real tree. Added a throwaway mis-tagged file
covering all four target failure modes (no tag, two layer tags on one class, tag only inside a
KDoc comment, untagged sibling class sharing a file with a correctly tagged class) — each failed
as expected — then deleted it. `./gradlew test` and `./gradlew :app:minifyReleaseWithR8` both pass
locally with no keystore present.

---

## Verification Notes

- `./gradlew ktlintCheck` — clean.
- `./gradlew test` — 204 tests, 0 failures, 0 skipped (baseline, and re-confirmed after the last
  mutation was reverted).
- `./gradlew :app:minifyReleaseWithR8` — succeeds; `mapping.txt` and the release `classes.dex`
  were read for Issue 1 and for the enum-persistence check under *What's Working*.
- No `androidTest` file was modified this pass, so no `connectedDebugAndroidTest` run was
  required. Every branch above that touches an instrumented test carries an explicit on-device
  run in its Tests line.
- No pre-existing flaky or environment-dependent failure surfaced during verification.
