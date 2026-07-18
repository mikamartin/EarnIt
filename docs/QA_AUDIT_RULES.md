# EarnIt — QA Audit Rules

A periodic, whole-suite audit of test quality — distinct from [CLEANUP_RULES.md](CLEANUP_RULES.md),
which runs after each unit of feature work and checks that one diff's tests are sound. This
checklist asks whether the *existing* suite, accumulated over many passes, still holds up:
would it actually catch a regression, does it match the spec's intent rather than just the
code's current behavior, and is it organized well enough to keep extending cheaply.

**Branch:** one branch per audit, `chore/qa-audit`, containing the updated
[QA_AUDIT_BACKLOG.md](QA_AUDIT_BACKLOG.md) plus any doc-hygiene fixes the audit surfaces in
TESTING.md/CLEANUP_LOG.md. Findings that require code changes (not just doc fixes) become
separate proposed follow-up branches in the backlog — do not fix everything inline on the audit
branch itself; each is its own logical unit of work.

## Checklist

### 1. CI/test-execution integrity
- [ ] List every test class in `app/src/test/` and `app/src/androidTest/`.
- [ ] Diff that list against every `classes:` entry in `.github/workflows/*.yml`'s matrix
      (or equivalent CI test-selection config).
- [ ] Flag any test class that exists but never runs in CI — this is a distinct failure mode
      from "no test exists" and easy to miss because the build stays green.

### 2. Mutation spot checks
- [ ] Select 6-10 unit test files spanning risk tiers: pure logic (no mocks), MockK-mocked
      repository/ViewModel, Robolectric-backed. Prioritize files backing core mechanics (points,
      gating, claim, import/export) and anything touched by recent feature work.
- [ ] For each: introduce one small, targeted mutation in the source under test (flipped
      boolean, off-by-one on a boundary, swapped operator, reordered priority). Run that file's
      tests via `./gradlew :app:testDebugUnitTest --tests "fully.qualified.ClassName"`. Confirm
      a clear failure. Revert immediately before moving to the next file.
- [ ] Record every result — pass (mutation caught) or fail (mutation missed) — in
      QA_AUDIT_BACKLOG.md's Mutation Check Results table, even if all pass. A miss usually means
      either a duplicated/untested code path (as opposed to the one the test actually exercises)
      or a genuinely weak assertion — trace it to which before concluding.
- [ ] `git status` must be clean before committing — every mutation is transient.

### 3. Spec cross-reference
- [ ] Check `EARNIT_SPEC.md`'s documented core mechanics (point formula, gating rules, cascade
      semantics, import merge/replace semantics, or whatever the current spec's headline
      contracts are) against the corresponding test files' actual assertions.
- [ ] Confirm each test encodes the spec's *stated* behavior, not just whatever the code
      currently happens to do — a risk when tests are written test-after rather than test-first.
- [ ] Flag any mismatch, or any core-mechanic test with no clear spec anchor.

### 4. Structural assertion review
- [ ] Sample the largest and newest instrumented/UI test files (by line count and by recency in
      `git log`).
- [ ] Check each against CLEANUP_RULES.md's own "assert after each action, don't chain several
      state-changing actions before checking any landed" rule — that rule is enforced on new
      diffs going forward; this checks whether the accumulated suite actually holds to it.
- [ ] Check for under-asserting tests: does the test edit/change N things but only verify fewer
      of them survived? A test whose name claims full coverage ("updatesFieldsAndPersists")
      should verify every field it touched, not a subset.

### 5. UI-logic-that-could-be-a-unit-test review
- [ ] Grep composables for inline validation/transformation logic living directly in
      `onValueChange`/`onClick` lambdas (character caps, digit filters, toggle-reset patterns,
      any small pure transformation) that's exercised only through full instrumented UI tests.
- [ ] For each candidate, confirm it's genuinely pure (no Compose/Context/Android dependency) —
      if so, it's a candidate for extraction into a plain function with a direct unit test,
      following the project's own `DragReorder`/`PugslyGesture`/`WidgetActionButton` precedent.
- [ ] Note duplication too: the same inline transformation reimplemented independently in more
      than one composable (not shared) is the same class of drift `DragReorder` was extracted to
      fix — call it out even if extraction isn't proposed yet.

### 6. Test duplication / helper-library review
- [ ] Check whether common multi-step flows (creating a fixture entity, waiting for a specific
      screen, standard dialog-cancel assertions) are copy-pasted inline across many UI test files
      instead of shared via a helper file or base class, mirroring what `RoomIntegrationBase`
      and `ViewModelTestBase` already do for their tiers.
- [ ] A duplicated helper redefined per-file (rather than imported) is the concrete signal to
      look for.

### 7. TESTING.md hygiene
- [ ] Recompute aggregate test counts (grep every `@Test` per file) and compare against every
      aggregate figure TESTING.md states (pyramid, section headers, cadence table) — these tend
      to drift independently since only some are updated per pass.
- [ ] Check Edge Case / narrative entries for bug-discovery history (how a bug was found, what
      the fix was) rather than current behavior — against `CLAUDE.md`'s documentation rule.
      History belongs in `CLEANUP_LOG.md`, referenced by pass number, not narrated inline.
- [ ] Check every `Pass N` cross-reference (in TESTING.md, DEV_PLAYBOOK.md, or elsewhere) is
      still within CLEANUP_LOG.md's retained 3-most-recent window; flag any dangling reference.
- [ ] Check CLEANUP_LOG.md's own entries are in the order its header describes (oldest retained
      pass first, newest at the bottom).

### 8. Record findings
- [ ] Write or refresh `docs/QA_AUDIT_BACKLOG.md`: a short "What's Working" summary (issues get
      their own section — this one is for what held up under scrutiny, e.g. mutation checks
      caught, spec alignment confirmed, structural review clean), numbered Issues Found,
      Mutation Check Results table, Spec Cross-Reference Notes, and a "Work, Grouped by Branch"
      section — one subsection per proposed follow-up branch, each with a Deliverable, Steps,
      and Tests line, so it can be picked up directly without re-deriving context.
- [ ] Before assuming an old backlog item is still open, check `git log`/merged PRs for its
      follow-up branch.
- [ ] When a backlog item is resolved: don't delete it. Condense its Issues Found entry to one
      sentence stating what it was and that it's fixed (keep the heading/number so it stays
      findable). Condense its Work Item section to a short, dry summary of what was actually
      done in place of the Steps list, and mark the section heading `(done)`.
- [ ] Apply the doc-hygiene fixes from step 7 directly on the audit branch; leave every
      code-level finding (formula fixes, extraction refactors, CI matrix fixes) as a proposed,
      not-yet-started branch in the backlog.

## Verification

Same build/lint/test/commit discipline as any other change — see `CLAUDE.md`.
