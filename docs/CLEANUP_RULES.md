# EarnIt — Cleanup Rules

Run this checklist after any significant feature work or refactor. Copy it into a new entry in [CLEANUP_LOG.md](CLEANUP_LOG.md), tick off what you found and fixed, note anything deferred with a reason.

## Log retention

`CLEANUP_LOG.md` keeps only the **3 most recent** passes. When a new pass is added and the log has more than 3 entries, delete the oldest one(s) so 3 remain. Full history isn't lost — every past change is tracked in git history and in merged PRs on GitHub. `CLEANUP_LOG.md` is a rolling snapshot of the latest cleanup work, not a permanent archive.

## Checklist

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
