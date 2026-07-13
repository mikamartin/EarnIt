# EarnIt — Developer Playbook

---

## 1. Post-Work Cleanup

Run this after any significant feature work or refactor. Copy the checklist into a new entry in [CLEANUP_LOG.md](CLEANUP_LOG.md), tick off what you found and fixed, note anything deferred with a reason.

### Duplication
- [ ] Are any composables copy-pasted with minor variation? Extract a shared component or parameter.
- [ ] Are any styling patterns (colors, gradients, padding sequences) repeated inline instead of using the design system (`LocalEarnItAccents`, `EarnItButtons.kt`, theme colors)?
- [ ] Are any strings duplicated across `Strings.kt` and call sites?
- [ ] Does any ViewModel logic appear in more than one place?
- [ ] Does any new `Repository` function overlap with an existing one that could be parameterised instead?
- [ ] Does any new `Dao` query duplicate the result of an existing query with a Kotlin-side filter that could be pushed into SQL?

### Decoupling
- [ ] Do composables contain business logic that belongs in the ViewModel or Repository?
- [ ] Does the ViewModel directly reference UI types (Color, Dp, Composable functions)?
- [ ] Does the data layer (`Repository`, `Dao`) reference ViewModel or UI concerns?
- [ ] Are new screens receiving the full ViewModel when they only need a subset of state/actions? Consider passing specific lambdas or state instead.

### Complexity & Pattern Health
- [ ] Are there composables over ~150 lines that could be split into focused sub-composables?
- [ ] Are there deeply nested lambdas or modifier chains that are hard to follow?
- [ ] Are `LaunchedEffect` keys correct — do they re-trigger exactly when needed and no more?
- [ ] Is `remember` vs `rememberSaveable` correct for each piece of state? (Saveable = survives rotation.)
- [ ] Are coroutine scopes (`rememberCoroutineScope`, `viewModelScope`) used in the right layer?
- [ ] Do new buttons, dialogs, rows, or list items use the established components (`EarnItPrimaryButton`, `EarnItOutlinedButton`, `RadioRow`, `CollapsibleGroupHeader`, `AboutActionRow`, etc.) rather than reimplementing equivalent layouts inline?
- [ ] Does any new composable reimplement something M3 already provides (custom checkbox, custom progress bar, custom switch behaviour)?
- [ ] Does any new helper or extracted composable have only one caller? If so, is the extraction actually earning its keep, or would the code be clearer inline?
- [ ] Could any new helper absorb nearby duplication that already existed before this change?

### Dead Code & Hygiene
- [ ] Are there unused imports, variables, parameters, or functions? (Check IDE warnings.)
- [ ] Are there commented-out code blocks that should be deleted?
- [ ] Are there resources (drawables, strings, colors) that are declared but never referenced?
- [ ] Are there TODO/FIXME comments that have since been resolved?
- [ ] Are there test/debug helpers (seed data, logging functions) still present that are marked for pre-release removal?
- [ ] Are there inline user-visible strings in composables (dialog titles, placeholders, empty-state messages) that should live in `Strings.kt`?
- [ ] Is `git status` clean — no stray untracked files that should be gitignored, nothing accidentally staged?

### Naming Consistency
- [ ] Do new files follow the established naming pattern (`*Screen.kt`, `*ViewModel.kt`, `*Repository.kt`)?
- [ ] Does any new file sit in the right package (`data/`, `di/`, `ui/`, `viewmodel/`, `widget/`) rather than loose at the package root?
- [ ] Do new composables use consistent naming (PascalCase, descriptive, no abbreviations)?
- [ ] Do new constants live in `Strings.kt` and follow the existing naming style?
- [ ] Does any symbol name conflict with or shadow a standard library or Compose name?

### Hardcoded Values
- [ ] Are new colors hardcoded as `Color(0xFF...)` where a theme color or `LocalEarnItAccents` value should be used?
- [ ] Are new magic numbers (sizes, durations, thresholds) inline where a named constant would be clearer?

### Accessibility
- [ ] Do icon-only buttons (`IconButton` with no visible label) have a non-empty `contentDescription`?
- [ ] Are all tappable targets at least 48 dp × 48 dp?

### Deprecated APIs
- [ ] Do any new calls produce deprecation warnings in the IDE? Resolve or document with a reason.

### Spec Review
- [ ] Does [EARNIT_SPEC.md](EARNIT_SPEC.md) still accurately describe what was built? Walk through any sections touched by the work and verify the description matches the current behaviour.
- [ ] If the implementation diverged from the spec intentionally (better idea found during build, constraint discovered, UX changed) — update the spec to reflect reality.
- [ ] If the implementation diverged unintentionally (something was missed or done wrong) — log it as a bug or task to fix, do not silently update the spec to match broken behaviour.
- [ ] Are any new patterns, components, or flows undocumented in the spec? Add them.
- [ ] Were any Deferred Ideas implemented? Remove or update the corresponding entry in the Deferred Ideas section.

### Tests
- [ ] Does any new logic in `Repository` or `ViewModel` lack unit test coverage? Check the relevant test file; add cases if the new path isn't exercised.
- [ ] Were any existing `Repository` or `ViewModel` methods changed in a way that makes current tests pass for the wrong reason (e.g. mock expectations now match new signatures by coincidence)? Review affected test files, not just CI green.
- [ ] If a bug was fixed, is there a regression test that would have caught it?
- [ ] Were any features removed or renamed? Remove or update the corresponding tests so they don't silently pass against dead code.
- [ ] Do new edge cases belong to an existing test class, or do they warrant a new file? (New file threshold: 3+ tests for a cohesive new behaviour.)
- [ ] Does any test chain several state-changing actions before checking any of them landed, or re-verify logic already covered by an existing test? Assert after each action instead — pinpoints which step broke and avoids stacking unverified interactions.
- [ ] Were any new instrumented tests added? Confirm they run on a device before committing (a test that never ran may have a compile error hidden by Gradle's incremental build).
- [ ] Did this change touch `AppModule`, `TestAppModule`, or add a new `@Inject` site? Run `./gradlew assembleDebugAndroidTest` — `test` and `assembleDebug` don't compile the androidTest variant, so a binding missing only from `TestAppModule` (which fully replaces `AppModule` for instrumented tests) won't surface otherwise.
- [ ] Is [TESTING.md](TESTING.md) still accurate?
  - Update aggregate counts (test pyramid, section headers, cadence table) if they changed, rounded to the nearest 5 or 10 rather than an exact-looking figure — they're a ballpark, not a maintained tally, and false precision just creates more numbers to keep in sync. Per-file counts in the table rows stay exact since they map directly to that file's `@Test` methods.
  - Add a row to the relevant table for any new test file.
  - Move items out of **Deferrals** if they are now covered.
  - Add new gaps to **Deferrals** with a reason if this pass knowingly skips coverage.
- [ ] Does any new flow cross a system-process boundary instrumented tests can't drive (system file picker, widget activity chain, Play Store-only API)? Add it to [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) with rationale, cadence, and steps instead of leaving it untested.

---

## 2. Ship Checklist

Items are independent — work them in any order. Strip completed items as they are done — this list should only ever contain open work.

### Before first release

**Play Store**
- [ ] Short and long store description
- [ ] Content rating questionnaire completed and data-safety form submitted (truthfully: no data collected, all local)

**Closed Testing**
- [ ] Share [CLOSED_TESTING_GUIDE.md](CLOSED_TESTING_GUIDE.md) with testers when the Play closed testing track opens

### Post-launch

- [ ] CI badge + Play Store link in README (once CI and store listing are live)
- [ ] Test result artifacts published; badge(s) in README
- [ ] Stretch: auto-upload AAB to Play internal testing track (r0adkll/upload-google-play or Fastlane)

---

## 3. How to Cut a Release

Run through this after CI is green on `main` and the Manual Test Plan has passed.

1. Confirm `versionCode` and `versionName` in `app/build.gradle.kts` are correct for this release — increment `versionCode` by 1 and update `versionName` to match the tag (e.g. `"1.0.0"`). Commit and push; let CI go green.
2. Pull the latest `main` locally:
   ```
   git checkout main
   git pull origin main
   ```
3. Create and push the version tag — this triggers Workflow 3:
   ```
   git tag v1.0.0
   git push origin v1.0.0
   ```
4. Go to GitHub → **Actions** → watch the Release workflow run. On success, download the signed AAB from the workflow's Artifacts section.
5. Upload the AAB to Play Console manually (Internal Testing → Production track, depending on the release type).

**Version naming:** tags follow `vMAJOR.MINOR.PATCH`. `versionCode` must increment on every Play Store upload — Play rejects a lower or equal code. `versionName` is what users see (e.g. "1.0.0").

**To delete a test tag** (e.g. after a trial run):
```
git tag -d v0.0.1-test
git push origin --delete v0.0.1-test
```

---

## 4. Known Limitations

Permanent, accepted constraints — not open work, nothing here gets checked off. Document in release notes or the store listing if relevant. For testing gaps that are manual by design rather than a temporary environment issue, see [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) instead — this section is for product/code constraints, not test strategy.

- Widget colors hardcoded warm-gold — Glance limitation.
- Progress bar track backgrounds (`Color(0xFFFFFBF0)`, `Color(0xFFFFF5DC)`), disabled LOG button fill and border, detail dividers (`Color(0xFFD5C9B0)`), the activity-log task name color (`Color(0xFF8E7CC3)`), and the reward target cost label (`Color(0xFFB06000)`) are hardcoded hex values that do not adapt to Ocean Blue or Forest themes — intentional design choices, not bugs.
- `taskState` (RewardEditScreen) and `rewardLinkState` (TaskEditScreen) lose checkbox state on rotation — would require a custom `mapSaver`.
- `InfoIconButton` (`EarnItButtons.kt`) uses a 24dp touch target, below the 48dp accessibility minimum — accepted trade-off for a small, low-frequency toggle. Restoring the full 48dp visibly grows the row (Compose's `minimumInteractiveComponentSize()` reports the enlarged size to the parent layout, not just the touch system) and adds noticeable extra spacing above the note it reveals on `SettingsScreen`'s name/rewards/tasks toggles — caught in manual testing on Pass 36. Comparable to the `AddTaskToRewardDialog` 32dp exception (Pass 5), but permanent rather than pending revisit.
- `StandardContent` in `EarnItWidget.kt` has no general overflow protection: content is a fixed-height `Column` centered inside a fixed-size `Box` (`fea294c`, kept to avoid dead space on tall launcher grid cells), so if rendered content height ever exceeds the actual widget box, `Alignment.Center` crops evenly off the top and bottom with no scroll or shrink fallback. `fix/widget-hint-overflow` bounded the one known trigger (the mandatory-task hint wrapping to 2 lines on narrow widths) with `maxLines = 1`, but any other future source of extra height — a longer reward name at large accessibility font scale, a new line added to the layout — would reproduce the same clipping. Revisit with a proper responsive layout (e.g. `LocalSize`-driven, matching the pattern already used in `ProgressBar`) if this recurs.

---

## 5. Tooling Upgrade Reference

Update this section each upgrade cycle. The version matrix and gotchas below reflect the most recent upgrade; update in place rather than appending.

`gradle/libs.versions.toml` is the source of truth for dependency and plugin versions — update it there first, then reflect the change in the matrix below. The one exception is the `buildscript classpath` Kotlin override (gotcha 1), which must stay a literal since catalog accessors aren't available in that block.

### Current working version matrix

| Tool | Version | Constraint |
|---|---|---|
| AGP | 9.2.0 | Requires Gradle 9.4.1+ |
| Gradle | 9.4.1 | Minimum for AGP 9.2 |
| Kotlin | 2.3.20 | Pinned via `buildscript classpath` (see gotcha 1) |
| KSP | 2.3.9 | Decoupled from Kotlin versioning since KSP 2.3.0 |
| Hilt | 2.59.2 | First version supporting and requiring AGP 9 |
| Room | 2.8.4 | Required for Kotlin 2.3.x KSP2 compatibility |
| foojay-resolver-convention | 1.0.0 | Pre-1.0 versions reference Gradle 9-removed internals |
| Compose BOM | 2026.05.00 | |
| ktlint (org.jlleitschuh.gradle.ktlint) | 14.2.0 | Default style enforced; see `.editorconfig` for the two narrow exceptions (`@Composable` naming, test naming) |

### Gotchas

**1. AGP 9 built-in Kotlin — classloader conflict**
AGP 9 provides built-in Kotlin (KGP 2.2.10). Adding an explicit `kotlin.android` plugin alongside it loads `ApplicationExtensionImpl` from two classloaders, producing a `ClassCastException` on every sync. Fix: remove `id("org.jetbrains.kotlin.android")` from all `plugins {}` blocks. To pin a higher Kotlin version, add to root `build.gradle.kts`:
```kotlin
buildscript {
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20") }
}
```

**2. `kotlinOptions` removed**
Without the `kotlin.android` plugin, `kotlinOptions { jvmTarget }` inside `android {}` is no longer available. AGP 9 automatically aligns the Kotlin JVM target to `compileOptions`. Delete the `kotlinOptions` block entirely.

**3. Hilt minimum version for AGP 9**
Hilt 2.51.1–2.58 fail with AGP 9. Hilt 2.59.x is the first release that supports and requires AGP 9.

**4. Room KSP2 crash on Kotlin 2.3.x**
`room-compiler` below 2.8.x crashes with `unexpected jvm signature V` when processing DAOs under Kotlin 2.3.x. Update all three Room artifacts (`room-runtime`, `room-compiler`, `room-ktx`) together to 2.8.x.

**5. foojay pre-1.0 incompatibility**
`foojay-resolver-convention` below 1.0.0 references `IBM_SEMERU` and `FoojayToolchainsPlugin`, both removed in Gradle 9. Update to 1.0.0 in `settings.gradle.kts`.

**6. Compose API removals**
`animateItemPlacement()` was removed from Compose Foundation — replace with `animateItem()` in all `LazyColumn`/`LazyRow` item scopes. Check BOM release notes for other removals when bumping.

### Next upgrade checklist
- [ ] Check AGP ↔ Gradle compatibility matrix before changing either
- [ ] Check Hilt release notes for AGP compatibility (github.com/google/dagger/releases)
- [ ] Check KSP releases for Kotlin compatibility (github.com/google/ksp/releases)
- [ ] Bump Room, Hilt, and foojay together with AGP — they have hard minimum version dependencies
- [ ] Do the upgrade on a dedicated branch; expect 3–5 sync/build errors on a major version jump
- [ ] Run `./gradlew assembleDebug` from terminal to confirm — IDE sync errors and build errors differ
- [ ] Run tests after a clean build to catch silent regressions

---

## 6. Database Schema Migrations

The DB schema was reset to `version = 1` as the launch baseline (versions 1–10 were internal dev-only churn — renames, added columns — with no real install to ever migrate; see `CLEANUP_LOG.md`). From this baseline onward, the app has (or will soon have) real installs, so the rule changes:

**Every future bump to `EarnItDatabase.kt`'s `version` must ship a real `Migration(oldVersion, newVersion)`, registered via `.addMigrations(...)` in `AppModule.kt`.**

Why this matters: `EarnItDatabase`'s builder also has `.fallbackToDestructiveMigration(dropAllTables = true)`. That fallback exists to keep local dev/emulator resets convenient — but if a version bump ships to a real device without a matching migration, it silently drops every table: every task, reward, and the permanent History log a user has earned, gone, with no error and no cloud backup as a safety net (Android auto-backup is conditional — daily, charging, Wi-Fi — not a guarantee).

Treat "forgot to write the migration" as a release-blocking bug, not something the destructive fallback should be relied on to absorb. When adding a `Migration`, add a matching entry to `EARNIT_SPEC.md`'s schema version note and, where practical, a manual verification pass (install old schema → upgrade → confirm data survives) before merging — there's no automated migration test harness in this project yet (`exportSchema = false`, no `MigrationTestHelper` usage), so this is presently a manual discipline, not a CI-enforced one.
