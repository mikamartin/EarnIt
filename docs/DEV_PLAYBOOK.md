# EarnIt — Developer Playbook

The post-work cleanup checklist lives in [CLEANUP_RULES.md](CLEANUP_RULES.md).

---

## 1. Ship Checklist

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

## 2. How to Cut a Release

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

## 3. Known Limitations

Permanent, accepted constraints — not open work, nothing here gets checked off. Document in release notes or the store listing if relevant. For testing gaps that are manual by design rather than a temporary environment issue, see [MANUAL_TEST_PLAN.md](MANUAL_TEST_PLAN.md) instead — this section is for product/code constraints, not test strategy.

- Widget colors hardcoded warm-gold — Glance limitation.
- Progress bar track backgrounds (`Color(0xFFFFFBF0)`, `Color(0xFFFFF5DC)`), disabled LOG button fill and border, detail dividers (`Color(0xFFD5C9B0)`), the activity-log task name color (`Color(0xFF8E7CC3)`), and the reward target cost label (`Color(0xFFB06000)`) are hardcoded hex values that do not adapt to Ocean Blue or Forest themes — intentional design choices, not bugs.
- `taskState` (RewardEditScreen) and `rewardLinkState` (TaskEditScreen) lose checkbox state on rotation — would require a custom `mapSaver`.
- `InfoIconButton` (`EarnItButtons.kt`) uses a 24dp touch target, below the 48dp accessibility minimum — accepted trade-off for a small, low-frequency toggle. Restoring the full 48dp visibly grows the row (Compose's `minimumInteractiveComponentSize()` reports the enlarged size to the parent layout, not just the touch system) and adds noticeable extra spacing above the note it reveals on `SettingsScreen`'s name/rewards/tasks toggles — caught in manual testing on Pass 36. Comparable to the `AddTaskToRewardDialog` 32dp exception (Pass 5), but permanent rather than pending revisit.
- `StandardContent` in `EarnItWidget.kt` has no general overflow protection: content is a fixed-height `Column` centered inside a fixed-size `Box` (`fea294c`, kept to avoid dead space on tall launcher grid cells), so if rendered content height ever exceeds the actual widget box, `Alignment.Center` crops evenly off the top and bottom with no scroll or shrink fallback. `fix/widget-hint-overflow` bounded the one known trigger (the mandatory-task hint wrapping to 2 lines on narrow widths) with `maxLines = 1`, but any other future source of extra height — a longer reward name at large accessibility font scale, a new line added to the layout — would reproduce the same clipping. Revisit with a proper responsive layout (e.g. `LocalSize`-driven, matching the pattern already used in `ProgressBar`) if this recurs.

---

## 4. Tooling Upgrade Reference

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

## 5. Database Schema Migrations

The DB schema was reset to `version = 1` as the launch baseline (versions 1–10 were internal dev-only churn — renames, added columns — with no real install to ever migrate; see `CLEANUP_LOG.md`). From this baseline onward, the app has (or will soon have) real installs, so the rule changes:

**Every future bump to `EarnItDatabase.kt`'s `version` must ship a real `Migration(oldVersion, newVersion)`, registered via `.addMigrations(...)` in `AppModule.kt`.**

Why this matters: `EarnItDatabase`'s builder also has `.fallbackToDestructiveMigration(dropAllTables = true)`. That fallback exists to keep local dev/emulator resets convenient — but if a version bump ships to a real device without a matching migration, it silently drops every table: every task, reward, and the permanent History log a user has earned, gone, with no error and no cloud backup as a safety net (Android auto-backup is conditional — daily, charging, Wi-Fi — not a guarantee).

Treat "forgot to write the migration" as a release-blocking bug, not something the destructive fallback should be relied on to absorb. When adding a `Migration`, add a matching entry to `EARNIT_SPEC.md`'s schema version note and, where practical, a manual verification pass (install old schema → upgrade → confirm data survives) before merging — there's no automated migration test harness in this project yet (`exportSchema = false`, no `MigrationTestHelper` usage), so this is presently a manual discipline, not a CI-enforced one.
