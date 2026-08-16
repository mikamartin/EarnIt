# First-launch onboarding tutorial ("Pugsly's Quest Scroll")

> Working plan for the `feature/onboarding-tutorial` branch. Not a source-of-truth doc — delete this file once the feature ships and merges.

## Context

A live test session surfaced a real gap: the user mixed up EarnIt's model (earn a *want*, guilt-free, by doing tasks you'd otherwise dodge) with a regular to-do app. Two specific failures showed up:
1. Trouble choosing a reward — defaulted to something too small/easy to feel like it needed "permission" (e.g. coffee), and struggled to even name a want.
2. Trouble choosing tasks — added step-like to-do items instead of the task they actually avoid, so the mandatory-task gate didn't feel like it was doing anything.

There's currently no onboarding of any kind in the app (confirmed: no screens, ViewModels, or flags for first-launch/tutorial anywhere in the codebase). This plan adds a first-launch tutorial that teaches by having the user actually build their first real reward and task, rather than a passive slideshow, using the existing Pugsly mascot as the guide and the app's Warm Gold theme.

## Concept

Five-step guided flow, gated behind a "seen onboarding" flag, replayable from Settings. Steps 0 and 4 are full-screen beats; steps 1-3 are a coach-mark/spotlight overlay riding on top of the **real** Create Reward screen (`RewardEditScreen.kt`) — the user leaves with an actual reward and task set up, not a demo of one.

**Step 0 — Permission beat** (full-screen, gold gradient background, no real UI yet)
Pugsly bounces in (reuse existing bounce animation), 3 tap-to-advance lines:
> "So. You want something."
> "And some annoying little voice in your head says you haven't *earned* it yet."
> "That voice isn't wrong. It's just... unhelpful. Let's fix that."
Ends with a gold wax-seal stamp animation, then navigates to Create Reward.

**Step 1 — Name the want** (spotlight on the reward-name field)
> "Not a chore. Not a 'should.' A real want, the kind you'd normally talk yourself out of. What's yours?"
If the field is still empty after a beat, show dismissible starter chips that fill it on tap: `That 7-book series` · `The trip you keep postponing` · `Phone upgrade` · `Something dumb-but-delightful`.

**Step 2 — Price it** (spotlight on cost field)
> "Price it too cheap and you're not earning it, you're bribing yourself. Too brutal and you'll just quit. Pick fair."

**Step 3 — Link the task that counts** (spotlight on the link-tasks / mandatory-toggle area). Trimmed to 3 bubbles so this step, which carries the most teaching, doesn't overstay:
> "Pick a task you actually dodge."
> "You're not limited to one. Link as many tasks as you want to this reward, and set what each is worth."
> "Mandatory locks it as required to claim; non-mandatory just adds points. Repeatable tasks earn every time you log them, not just once. Mix and match."
> "Blanking? Task Library has ready-made ones, a couple taps to import."
If adding a task inline, a gut-check line under the task-name field: *"Would you skip this without the reward? That's usually the one worth linking."*

**Step 4 — Sealed** (full-screen outro)
Gold scroll/seal animation, confetti, final line:
> "You're allowed to want this. Now go earn it, guilt-free."
Drops the user on Home with their real reward + task live — nothing is claimable yet, this line grants permission to *pursue* it, not the reward itself.

## Step advancement, pointers, and progress

- **No silent auto-advance.** Each spotlight bubble (steps 1-3) shows an explicit "Continue" affordance that only lights up once that step's condition is met (name non-empty; cost valid; ≥1 task linked). This avoids the overlay jumping mid-type.
- **Step 3 doesn't get its own Save.** Once ≥1 task is linked, the bubble's "Continue" instead points at the real Save button already on `RewardEditScreen` (existing `canSave` logic, line 184) rather than adding a second, competing save action. The outro (step 4) is triggered off the existing save-success/navigation callback, not a separate overlay-owned button — one source of truth for "the reward is actually saved."
- **Real pointers, not just a dimmed hole.** The `SpotlightScrim` cutout is connected to its bubble with a short directional connector (chevron/line from bubble to cutout edge), since on small screens the two can be far apart and a bare hole in the scrim isn't always self-explanatory.
- **Progress indicator.** A small step-dot row (1-5) sits with each bubble, plus a one-line "next up" preview (e.g. "Next: set a fair price") so the user always has a sense of what's coming, not just what's active.

## Testing

Two new test files, matching this codebase's existing split between pure-logic unit tests and Compose UI tests (it does not unit-test simple DataStore flags directly — see `SettingsRepository.kt`'s dismiss flags, which have no dedicated unit tests):
- `app/src/test/java/com/earnit/app/ui/OnboardingStepTest.kt` — pure logic for the `OnboardingStep` state machine (transitions, per-step advance conditions), no Compose dependency. Mirrors `NudgeDeciderTest.kt` / `PugslyGestureTest.kt` / `PendingRewardIdTest.kt`.
- `app/src/androidTest/java/com/earnit/app/OnboardingFlowUiTest.kt` — end-to-end Compose UI test mirroring `RewardEditScreenUiTest.kt`: flow triggers on first launch (fresh settings), each step's spotlight targets the correct real field, starter-chip tap fills the name field, completing the flow sets the persisted flag and leaves a real reward+task on Home, and Settings → Replay Tutorial re-triggers it.

## Implementation

**Persistence** — follow the exact existing pattern in `app/src/main/java/com/earnit/app/data/SettingsRepository.kt`, which already stores one-time dismissible UI flags this way (`WIDGET_NUDGE_DISMISSED`, `SETTINGS_TIP_DISMISSED`, lines 37-38, with `dismissWidgetNudge()`/`dismissSettingsTip()` at lines 128-134):
- Add `ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")` to `Keys`.
- Add `onboardingSeen: Boolean = false` to `AppSettings.kt`, mapped in the `settings` Flow (mirror line 77's `widgetNudgeDismissed` handling).
- Add `suspend fun markOnboardingSeen()` and `suspend fun resetOnboarding()` to `SettingsRepository`, same idiom as the dismiss functions.
- In `EarnItViewModel.kt`, no new StateFlow needed — read `settings.value.onboardingSeen` directly (same pattern already used for `widgetNudgeDismissed` in `RewardDetailScreen.kt:200`). Add thin `markOnboardingSeen()` / `replayOnboarding()` wrappers.

**Trigger & entry points**
- In `EarnItApp.kt`, after settings are collected, a `LaunchedEffect(Unit)` checks `settings.value.onboardingSeen` once and kicks off the flow if false. The overlay is composed as a child of `RewardEditScreen` (not a separate NavHost route or top-level sibling `Box`), so it never adds its own back-stack entry — step 0 navigates to `Screen.RewardEdit.route(0L)` and the overlay takes over from there.
- "Replay Tutorial" row added to `SettingsScreen.kt` (near the About row, ~line 143), calling `viewModel.replayOnboarding()` then navigating Home. New strings added to `Strings.kt` following its existing flat `const val`/`fun` convention.

**New files** (package `com.earnit.app.ui.onboarding`)
- `OnboardingOverlay.kt` — top-level composable + `OnboardingStep` sealed state machine (Intro, Spotlight(NAME/COST/TASKS), Outro); step index in `rememberSaveable` so it survives recomposition during navigation.
- `SpotlightScrim.kt` — reusable coach-mark primitive: full-screen scrim `Canvas` with a cutout hole sized from a `Rect`, captured via `Modifier.onGloballyPositioned { it.boundsInWindow() }` on the real target fields. No existing scrim/cutout primitive in the codebase (`HomeScreen.kt`'s `Canvas` use is progress bars only) — this is new.
- `SpeechBubble.kt` — Pugsly avatar + dialogue + tap-to-advance, reusing the existing mascot drawable lookup and the `Animatable`/spring bounce from `HomeScreen.kt:337-344`.
- `IntroOutroScreens.kt` — full-screen gold-gradient beats for steps 0/4, reusing `LocalEarnItAccents.current.gradientStart/End` (already used in `HomeScreen.kt:121`).
- `StarterChips.kt` — the step-1 chip row.

**Wiring into the real form** — confirmed in `RewardEditScreen.kt`: `name` and `cost` are local `rememberSaveable` state (lines 106-107) with `onNameChange`/`onCostChange` callbacks already threaded to child composables (lines 250-258). The overlay is rendered as a child of `RewardEditScreen` and receives `name`/`onNameChange`/`cost`/`onCostChange` directly as params — starter chips call the existing `onNameChange`, no new state bus or cross-composable channel needed.

**Open risks to watch during implementation**
- `boundsInWindow()` only stabilizes after first layout and shifts on IME show/hide and rotation — the spotlight must re-read positions on every layout change, not cache once.
- Small/short screens: verify the reward-name field spotlight still reads correctly if the icon picker or keyboard is open.

## Files touched

- `app/src/main/java/com/earnit/app/data/SettingsRepository.kt` — add flag + methods
- `app/src/main/java/com/earnit/app/data/AppSettings.kt` — add field
- `app/src/main/java/com/earnit/app/ui/EarnItApp.kt` — trigger check
- `app/src/main/java/com/earnit/app/ui/RewardEditScreen.kt` — host the overlay, pass state
- `app/src/main/java/com/earnit/app/ui/SettingsScreen.kt` — Replay Tutorial row
- `app/src/main/java/com/earnit/app/ui/Strings.kt` — new copy constants
- New: `app/src/main/java/com/earnit/app/ui/onboarding/*.kt` (5 files above)

## Verification

- `./gradlew ktlintCheck` and `./gradlew test` (sequentially, per repo convention — never in parallel).
- `./gradlew assembleDebugAndroidTest` if any new `@Inject` sites are added to `SettingsRepository`/DI graph.
- Manual: fresh install (or `resetToDefaults()`/clear app data) to confirm the flow triggers on first launch, completes end-to-end with a real reward+task left on Home, and that Settings → Replay Tutorial re-triggers it correctly. Test on a small-screen device/emulator for the spotlight-positioning risk above.
