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

### 1. `RewardTaskCrossRef` is obfuscated by R8, and Moshi serialises it reflectively — JSON export/import of reward↔task links is unsound in the shipped release build

**Severity: high.** This is on the data-restore path of a live production release.

`JsonExport` builds Moshi with `addLast(KotlinJsonAdapterFactory())`. Only `EarnItExport` carries
`@JsonClass(generateAdapter = true)`; all five element types inside it (`TaskEntity`,
`RewardEntity`, `RewardTaskCrossRef`, `CompletionLogEntity`, `HistoryEntryEntity`) fall through to
the **reflective** adapter, which derives JSON keys from Kotlin metadata.

`proguard-rules.pro` protects four of the five via `-keep class com.earnit.app.data.*Entity { *; }`.
`RewardTaskCrossRef` does not end in `Entity`, and the `-keep class com.earnit.app.data.EarnItExport$*`
rule only covers nested classes, so nothing keeps it. Confirmed against
`app/build/outputs/mapping/release/mapping.txt`:

```
com.earnit.app.data.RewardTaskCrossRef -> gk2:
    long rewardId    -> a
    long taskId      -> b
    boolean isMandatory  -> c
    boolean isRepeatable -> d
```

versus `com.earnit.app.data.TaskEntity -> com.earnit.app.data.TaskEntity` (kept) for contrast.

So in the minified release build the reward↔task link rows are serialised through R8-rewritten
metadata rather than the source property names. `rewardId` and `taskId` have no defaults, so they
are *required* parameters for the reflective adapter — meaning a backup whose
`rewardTaskCrossRefs` entries use the documented key names will fail to parse, surfacing to the
user as `ImportInvalidJsonException` → **"File is not valid JSON"**, aborting the entire import.
Because R8's naming is not stable across builds, a backup exported by one release may also fail to
import into the next.

The exact runtime symptom (obfuscated keys written on export vs. hard parse failure on import)
needs one export→import round-trip on an *installed release build* to pin down. Either way the
keep rule is missing, the emitted format diverges from `EARNIT_SPEC.md` §7, and nothing tests it.

Why nothing caught it: `ExportImportTest`, `JsonExportTest`, and `JsonImportValidationTest` all
run against unminified debug/JVM classes. `MANUAL_TEST_PLAN.md`'s "Export / Import — full UI
round-trip" journey is the only backstop and does not specify a release build. CI never builds
the minified variant at all.

### 2. Wrong-file import can silently wipe all user data in Replace mode

**Severity: high.** Also a live data-loss path.

`JsonExport.fromJson` gates on a raw substring search — `earnItKeys.none { json.contains("\"$key\"") }`
— then parses with Moshi, which ignores unknown top-level keys. A foreign JSON file that merely
contains one of the five key names as a real top-level key with an empty array passes both stages
and yields an all-empty `EarnItExport`. In Replace mode `importFromJson` then runs
`database.clearAllTables()` and inserts nothing.

Verified with a throwaway probe against the real `JsonExport` (since reverted, tree clean):

| Input | Result |
|---|---|
| `{"tasks":[],"projects":[{"title":"x"}]}` | **`Success(EarnItExport(all empty))`** → Replace wipes everything, reports "Replaced ✓" |
| `{"tasks":["buy milk","walk dog"]}` | `ImportInvalidJsonException` — data safe, but the message ("File is not valid JSON") misdescribes a schema mismatch |
| `{"note":"my \"tasks\" for today"}` | `ImportWrongSchemaException` — correctly rejected (the escaped quote defeats the substring match) |

An all-empty export is legitimately valid (a backup from a fresh install), so the fix isn't to
reject emptiness. `JsonExport.toJson` always emits all five top-level keys, so requiring **all
five** to be present at the parsed top level would accept every genuine EarnIt backup and reject
the probe case above.

This also makes `EARNIT_SPEC.md` §7's validation claim inaccurate: step 3 says the key check
"catches both wrong-shape JSON (an unrelated object, a JSON array) and unrecognizable malformed
text with one check, and prevents a wrong file from silently wiping user data in Replace mode."
The first and last clauses do not hold for an unrelated object that happens to use one of the five
key names.

### 3. `ClearCascadeTest`'s two cross-ref assertions are vacuous — the FK cascade is not actually verified

`deleteTask_removesCrossRefs_rewardHasNoTasks` and
`clearAllTasks_removesTasksAndCrossRefs_rewardStillExists` both assert
`progress.allTasks.size == 0`. But `allTasks` is built in `observeUiState` as
`crossRefs.filter { … }.mapNotNull { taskMap[it.taskId] }`, and `taskMap` comes from live tasks —
so once the task row is deleted the cross-ref is dropped from `allTasks` **whether or not the
cross-ref row still exists**. Both assertions would pass with the FK cascade disabled entirely.
`deleteReward_removesActiveLogsAndCrossRefs_taskStillExists` names cross-refs in its title and
asserts nothing about them at all.

This reconciles a documentation contradiction: `TESTING.md`'s Deferrals section is correct that FK
cascade behaviour is not verified ("An instrumented test … would close this gap. Not yet
written"), while its instrumented table row for `ClearCascadeTest` claims "`deleteTask` /
`deleteReward` cascade" and `DeleteCascadeTest.kt`'s header comment claims "Real cascade behaviour
is covered by `MANUAL_TEST_PLAN.md` / `TESTING.md`" — neither is true.

Cascade enforcement itself *is* on: the generated `EarnItDatabase_Impl.onOpen` emits
`PRAGMA foreign_keys = ON`, so the production behaviour is correct. The gap is purely that no test
would notice if it regressed.

### 4. `showsProgressNumbers`' `!canClaim` term is dead code, and its test passes for the wrong reason

`RewardProgress.showsProgressNumbers` is `!canClaim && totalPoints < reward.cost`. Since `canClaim`
requires `totalPoints >= reward.cost`, `totalPoints < cost` already implies `!canClaim` — the first
term can never change the result. Deleting it entirely leaves all 21 `RewardProgressTest` tests
green (verified). No test can catch this, by construction.

The consequence is that `showsProgressNumbers false when points meet cost and mandatory task
unlogged` — the test that reads as pinning the mandatory-gate interaction — is actually only
pinning `totalPoints >= cost`. The mandatory-task half of that scenario is unasserted.

### 5. The import schema-key check's exactness is untested

Weakening `json.contains("\"$key\"")` to `json.contains(key)` leaves both
`JsonImportValidationTest` (8 tests) and `JsonExportTest` (5 tests) green. The check that stands
between a wrong file and `clearAllTables()` has no test pinning the part that makes it discriminate
— which is how Issue 2 stayed invisible.

### 6. Two "persistsAfterRecreate" tests don't actually test persistence across recreate

`SettingsScreenUiTest.selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate` and
`cloudBackupToggle_defaultsOn_turnedOff_persistsAfterRecreate` both call
`activityRule.scenario.recreate()` and then assert against `settingsRepository.settings.first()`.
DataStore was already written before the recreate, so both tests pass identically with the
`recreate()` line deleted. What they claim to cover — that the UI re-reads the persisted value
after the activity is rebuilt — is not asserted. Contrast
`SettingsUiTest.colorScheme_selectionPersistsAfterRecreate`, which checks the rendered result.

### 7. `useRandomNickname` test under-asserts its own claim

`SettingsScreenUiTest.useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting` only asserts
that `"Earn It, Zorro!"` is *absent* after enabling the toggle. A regression that made the greeting
render `"Earn It!"` with no name at all — or dropped the greeting entirely — would pass. Nothing
asserts a random nickname actually appears. `TESTING.md` repeats the overstated claim
("overrides the typed name on the greeting").

The same test also asserts the same condition twice: a `waitUntil { … isEmpty() }` immediately
followed by an `assertEquals(0, … .size)` that the wait already guaranteed.

Adjacent, smaller: `mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable` claims the
unlocked set is "exactly" Pugsly and Tabby but only checks that both names appear *somewhere*
(the row behind the dialog also renders "Pugsly", so the dialog's own cell is not isolated), that
Panda's hint shows, and that "Penguin" is absent. It never asserts the two are selectable, nor
that Panda's *name* is hidden.

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

### 12. `checkInstrumentedTestTags` has latent blind spots

The task reads each file's whole text and checks `text.contains("@UiTest")` etc. That means:

- **Not per-class.** A file holding two test classes where only one is tagged passes.
- **Comment-satisfiable.** A tag named in a KDoc block satisfies the check with no annotation
  applied.
- **Doesn't enforce "exactly one".** `LayerTags.kt`'s own KDoc says "Required on every instrumented
  test class — exactly one", but a class carrying both `@UiTest` and `@RepositoryTest` passes and
  then runs only in the UI shard (the `notAnnotation=UiTest` shard excludes it), silently losing
  its Repository-tier run.

No file currently trips any of these, so this is a latent gap rather than an active miss — but the
task is the sole mechanism keeping the annotation-filtered CI sharding honest.

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

- **`clearAllLogs_removesActiveAndArchivedLogs`** (`ClearCascadeTest`) asserts only
  `allLogs.size == 0`; it never asserts history entries were deleted, though `clearAllLogs` deletes
  both and `TESTING.md` describes the test as covering "active + archived logs **and** history
  entries". The DAO-call half is covered by `CleanupTest`, so this is a description accuracy issue
  more than a coverage one.
- **`clearAllDialog_confirm_wipesDataAndResetsSettingsExceptBackupChoice`** asserts theme,
  nickname, dev mode, mascots, and the backup toggle, but not `onboardingSeen` — which
  `resetForWipeEverything()` also resets, and which `EARNIT_SPEC.md` §6 calls out as a Wipe
  Everything reset path. The user-visible consequence (the first-launch tutorial reappearing on
  Home after a wipe) is spec-sanctioned but has no coverage at any layer.
- **`assertCancelClearsNothing`** (`CleanUpScreenUiTest`) checks task, reward, and log counts but
  not history entries, so the Clear Logs cancel path doesn't prove history survived.
- **The `logCompletion` archived-reward guard has no JVM-tier coverage.** Deleting
  `if (reward.isArchived) return@withTransaction` leaves the *entire* 204-test unit suite green.
  This is by design — `LogAgainstArchivedRewardTest` covers it at the instrumented Repository
  tier — but it means the guard is only protected on the emulator job, noted here so the
  dependency is explicit rather than incidental.

### 16. CI never builds the minified release variant

`ci.yml` runs `ktlintCheck`, `checkInstrumentedTestTags`, `test`, and `assembleDebug`.
`release.yml` runs `bundleRelease` — but only on a `v*.*.*` tag push, with no lint or test gate of
its own, and by then the release is being cut. Nothing exercises R8 on a PR, which is the tier
Issue 1 lives in and the reason a shrinker-only defect could ship. `minifyReleaseWithR8` needs no
keystore and completed locally in ~1 minute, so it is cheap to gate on.

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

- **§7 validation step 3** overstates what the schema check catches — Issue 2.
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

Seven proposed branches, ordered by severity. Nothing below has been started.

### `fix/moshi-crossref-keep-rule` — Issue 1

**Deliverable:** JSON export/import of reward↔task links behaves identically in debug and
minified release builds, and is proven to.

**Steps:**
1. Install the current release build (`bundleRelease`/`assembleRelease`) on a device and run one
   export → Replace-import round-trip on data that includes at least one reward↔task link, to pin
   the actual symptom before changing anything.
2. Fix the root cause rather than only the symptom. Preferred: add
   `@JsonClass(generateAdapter = true)` to all five export element types so Moshi codegen
   generates adapters and reflection drops out of the path entirely (this also lets
   `KotlinJsonAdapterFactory` and the `moshi-kotlin` dependency be removed if nothing else needs
   them). Fallback if codegen is rejected: replace the name-pattern keep rule with an explicit
   `-keep class com.earnit.app.data.RewardTaskCrossRef { *; }` — but note that a name-pattern rule
   is what failed here, so prefer removing the reflection dependency.
3. Audit the remaining reflective Moshi surface the same way — confirm via `mapping.txt` that every
   type reachable from `EarnItExport` keeps its property names.
4. Add a step to `MANUAL_TEST_PLAN.md`'s Export/Import journey requiring the round-trip be run on
   a **minified release build**, not a debug build, and say why.
5. If step 1 shows shipped v1.3.0 backups carry obfuscated keys, decide separately whether a
   one-time import compatibility shim is warranted; capture that decision in the PR.

**Tests:** Round-trip assertions already exist (`ExportImportTest`, `JsonExportTest`). Add a unit
test asserting the exact emitted top-level and per-entity key names for a fully populated
`EarnItExport`, so any future adapter/keep-rule change that alters the wire format fails at the
JVM tier instead of only on a real release install. Verify on-device per step 1.

### `fix/import-schema-validation` — Issues 2 and 5

**Deliverable:** A wrong file can no longer wipe user data in Replace mode, and the guard is
pinned by tests.

**Steps:**
1. Replace the substring gate in `JsonExport.fromJson` with a parsed top-level key check —
   read the JSON into a `Map<String, Any?>` and require all five expected keys, since
   `JsonExport.toJson` always emits all five. Keep `ImportWrongSchemaException` as the thrown type
   so the existing ViewModel mapping and UI strings are unaffected.
2. Distinguish the "recognisable EarnIt keys but wrong element shape" case from genuine syntax
   errors so `{"tasks":["buy milk"]}` reports a schema problem rather than "File is not valid
   JSON".
3. Update `EARNIT_SPEC.md` §7's validation list to describe what the check actually guarantees.

**Tests:** Extend `JsonImportValidationTest` with the three probe cases from Issue 2 (all-empty
foreign object with a `tasks` key, foreign `tasks` array of wrong shape, escaped-quote text) and a
case pinning that a genuine all-empty EarnIt export still succeeds. Add an `ExportImportTest` case
proving a Replace attempt with a foreign-but-key-matching file leaves existing rows intact —
mirroring the existing `importReplace_withWrongSchema_doesNotWipeExistingData`.

### `test/fk-cascade-and-cleanup-assertions` — Issues 3 and 15

**Deliverable:** The FK cascade is actually verified, and the cleanup tests assert everything
their names claim.

**Steps:**
1. Give `ClearCascadeTest` direct cross-ref reads (`RoomIntegrationBase` already exposes
   `database`, so `database.rewardTaskCrossRefDao().getAllCrossRefs()` is available) and assert row
   counts directly in `deleteTask_…`, `clearAllTasks_…`, and `deleteReward_…` instead of relying on
   `allTasks`.
2. Add history-entry assertions to `clearAllLogs_removesActiveAndArchivedLogs` and to
   `CleanUpScreenUiTest.assertCancelClearsNothing`.
3. Add the `onboardingSeen` assertion to
   `clearAllDialog_confirm_wipesDataAndResetsSettingsExceptBackupChoice`, and decide whether the
   tutorial reappearing on Home after a wipe warrants its own assertion.
4. Move FK cascade delete out of `TESTING.md`'s Deferrals, and correct `DeleteCascadeTest.kt`'s
   header comment to say where real cascade behaviour is verified.

**Tests:** This branch *is* tests. Run `connectedDebugAndroidTest` pinned to
`com.earnit.app.ClearCascadeTest` and `com.earnit.app.CleanUpScreenUiTest` on a device — a
strengthened assertion that was never executed proves nothing.

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

### `test/settings-persistence-and-assertions` — Issues 6 and 7

**Deliverable:** The recreate-persistence tests prove what their names claim, and the random-nickname
test asserts a positive outcome.

**Steps:**
1. Rewrite `selectedMascot_choiceOfUnlockedMascot_persistsAfterRecreate` and
   `cloudBackupToggle_defaultsOn_turnedOff_persistsAfterRecreate` to assert against the rendered UI
   after `recreate()` (the mascot shown on Home / the toggle's rendered state), following
   `SettingsUiTest.colorScheme_selectionPersistsAfterRecreate`. Keep the repository assertion as a
   secondary check if useful, but the UI assertion is the point.
2. Make `useRandomNickname_enabled_overridesTypedNicknameOnHomeGreeting` assert a random nickname
   is actually displayed — the greeting matches `Strings.appTitle(<some non-blank name>)` and is
   not the no-address form — rather than only that "Zorro" is gone. Drop the redundant
   `assertEquals` after the `waitUntil`.
3. Tighten `mascotPicker_defaultUnlockedSet_onlyPugslyAndTabbySelectable` to scope its Pugsly/Tabby
   lookups to the dialog (`isDialog()`, as `CancelDismissAssertions` already does) and to assert
   Panda's name is hidden.
4. Correct the corresponding `TESTING.md` row descriptions to match what the tests assert.

**Tests:** Run `connectedDebugAndroidTest` pinned to `com.earnit.app.SettingsScreenUiTest` on a
device.

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

### `chore/ci-release-build-gate` — Issues 12 and 16

**Deliverable:** CI exercises the shrinker on every PR, and the tag-triggered release can't build
from an ungated commit. The tag check makes `checkInstrumentedTestTags` per-class.

**Steps:**
1. Add a `./gradlew minifyReleaseWithR8` (or `assembleRelease`) step to `ci.yml`. It needs no
   keystore for the R8 step itself; if the signing config blocks it, gate on the
   `keystore.properties`-absent fallback already in `app/build.gradle.kts`.
2. Have `release.yml` run `ktlintCheck` and `test` before `bundleRelease`, so a tag pushed at an
   unvalidated commit fails loudly rather than producing a signed AAB.
3. Rewrite `checkInstrumentedTestTags` to scan per class declaration rather than per file: strip
   comments first, then require exactly one layer tag on each class that declares `@Test` methods,
   and at least one optional tag. Keep the existing failure message shape.

**Tests:** Add a deliberately mis-tagged throwaway file locally to confirm each new failure mode
actually fails (no tag, two layer tags, tag only in a comment, second untagged class in a tagged
file), then delete it. Confirm the R8 step passes on a PR before merging.

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
