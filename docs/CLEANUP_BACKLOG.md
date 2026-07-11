# EarnIt — Cleanup Backlog

A punch list from a whole-repo audit against [DEV_PLAYBOOK.md §1 Post-Work Cleanup](DEV_PLAYBOOK.md), run 2026-07-10 against `main` at `59d9a82` (post Pass 35 / PR #23, working tree clean).

Findings #1, #2, and #4–#8 from the original audit were actioned on `chore/cleanup-backlog-fixes` — see [CLEANUP_LOG.md Pass 36](CLEANUP_LOG.md) for what changed. Only the finding below remains open, tracked for its own branch (`refactor/split-oversized-screens`) since it's a structural split rather than a hygiene fix.

**Disposal:** once actioned, fold the result into a new numbered entry in `CLEANUP_LOG.md` and delete this file — it's a working list, not a permanent doc.

---

## Complexity & Pattern Health

### Five screens are single oversized composables with no internal extraction
Well past the ~150-line guideline, comparable in scope to the `EarnItApp.kt` split done in Pass 10 — likely warrants its own dedicated branch rather than folding into smaller fixes:

| File | Size |
|---|---|
| `RewardDetailScreen.kt` | 658 lines, one `@Composable fun RewardDetailScreen(...)` (was 694 at audit time; Pass 36's `LogPillButton` dedup removed 36 lines as a side effect) |
| `TaskEditScreen.kt` | ~637 lines in `TaskEditScreen(...)` before `EmojiPickerDialog`/`SliderRow` helpers |
| `SettingsScreen.kt` | ~430 lines in `SettingsScreen(...)` before the first helper |
| `RewardEditScreen.kt` | ~420 lines, one `@Composable fun RewardEditScreen(...)` |
| `HomeScreen.kt` | ~320 lines in `HomeScreen(...)` |

Not a new finding, just a size note: `DataScreen.kt` has grown to 398 lines (main composable ~263) since Pass 33's "~305 lines" mention — still only `NudgeDebugCard` extracted, consistent with that pass's prior review. No action implied unless it keeps growing.

---

## Reviewed with no findings (from the original audit, still accurate)

- **ktlint** — clean repo-wide (`./gradlew ktlintCheck` passed with zero violations at audit time).
- **Decoupling** — no ViewModel references to UI types, no Repository/Dao references to ViewModel/UI concerns.
- **State correctness** — `remember` vs `rememberSaveable` usage is correct throughout (all form-field state is saveable; plain `remember` uses are all derived/computed values). `LaunchedEffect` keys are all correctly scoped.
- **Dead code** — no TODO/FIXME comments, no commented-out code, no unused `Strings.kt` constants, no unused drawables, no unused private functions anywhere in `app/src/main/java/com/earnit/app`.
- **Naming consistency** — all screen files follow `*Screen.kt` convention and sit in the right package; no stdlib/Compose name shadowing.
- **Spec Screen Map (§10)** — matches the actual nav graph and screens; no drift.
- **Tip Jar deferred-idea status** — `FeatureFlags.TIP_JAR_ENABLED = false` matches the spec's "UI complete, hidden" description; `MockTipRepository`/`TipViewModel`/gated `AboutScreen` section all present as described.
- **`DEV_PLAYBOOK.md §4 Known Limitations`** — every item spot-checked and still accurate: widget colors, the four named hardcoded hex values (progress track, dividers, activity-log task name, reward cost label), and `taskState`/`rewardLinkState` still using plain `remember`.
- **`DEV_PLAYBOOK.md §2 Ship Checklist`** — unchanged, still fully open (store description, data-safety form, closed-testing guide share, CI badge/README, test-artifact publishing, AAB auto-upload stretch goal). No action implied here — just confirmed nothing was silently completed without the checklist being struck through.
- **`MANUAL_TEST_PLAN.md`** — correctly reflects the Pass 35 gesture-based dev-mode unlock; no stale reference to the old 7-tap About-screen trigger.
- **Other `TESTING.md` Deferrals** — none appear closed by recent commits (last 15 are all nudge-feature and dev-unlock-gesture work; none touch widget-refresh injection, RevenueCat, group-view UI, transaction rollback testing, or FK cascade coverage).
