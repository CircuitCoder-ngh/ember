# Ember

A gamified daily accountability app for Android (Kotlin + Jetpack Compose). Define the goals that make up
your best day, check them off, and watch your streak, XP and calendar fill in. Everything stays on the phone.

## How it works

- **Daily goals** recur on the weekdays you choose. **One-off goals** live on a single date.
- **Weekly and monthly goals** have a target per period ("Gym 3× a week", "300 pages a month"). Log progress on
  any day; they count toward the week/month score, not the daily score or streak.
- Goals are **check-off** or **count-up** (e.g. "Read 20 pages": 12/20 = 60% credit).
- Each day gets a **score** = weighted average credit of the goals scheduled that day. Days with nothing
  scheduled are rest days and are excluded from averages, not counted as zero.
- The **streak** continues while a day's score meets your threshold (default 80%). Perfect days (100%) get
  extra recognition. Today never breaks a streak until the day rolls over.
- Milestones (3, 7, 14, 30, 50, 100, …) award **streak freezes** (max 2) that automatically cover a missed day.
- Breaking a streak of 3+ days opens a **comeback quest**: hit your bar 3 days in a row and earn a freeze back.
  A miss during the quest restarts its progress; rest days are neutral.
- **XP** per goal, a perfect-day bonus and a streak multiplier feed **levels** and **badges**.
- **Editing a goal never rewrites history.** Each edit closes the old definition yesterday and opens a new one
  today, so past days always show the goal exactly as it was.
- Streak, XP and badges are recomputed from history every time, so fixing a past day fixes everything.
- Optional **hourly progress card**: a silent notification listing what's still open today, your %, streak and a
  quote, refreshed every hour and whenever you check something off. Optional **daily nudge** at a set time.
- Huge one-off goals are starred on the calendar and listed under **Coming up**.

## Layout

```
app/src/main/java/com/nhowe/ember/
  domain/engine/     pure Kotlin: DayPlanResolver, Scoring, StreakEngine, XpEngine, BadgeEngine, Engine
  domain/model/      Goal, GoalVersion, DayPlan, StreakState, XpState, Badge, EngineSnapshot …
  data/db/           Room 3 entities + DAOs (goal, goal_version, completion, day_override, celebration)
  data/repo/         GoalRepository (versioning rules), ProgressRepository, HistoryRepository, SettingsRepository
  data/backup/       JSON export/import
  ui/                Compose screens: today, calendar, stats, goals, settings, onboarding, celebration
  notifications/     inexact daily reminder (AlarmManager) + boot receiver
app/src/test/        JUnit tests for the engines and the backup codec
scripts/             setup-toolchain.sh, adb-connect.sh, deploy.sh
```

## Building (WSL, no Android Studio)

```bash
scripts/setup-toolchain.sh        # once: JDK 17, Android SDK, Gradle wrapper under ~/opt (no sudo)
source ~/.bashrc
./gradlew :app:testDebugUnitTest  # engine tests
./gradlew assembleRelease         # signed with ~/keys/ember-release.jks (see below)
```

Release signing credentials live in `~/.gradle/gradle.properties` (`EMBER_STORE_FILE`, `EMBER_STORE_PASSWORD`,
`EMBER_KEY_ALIAS`, `EMBER_KEY_PASSWORD`). A copy of the keystore and those properties is kept at
`/mnt/c/Users/nghho/accountabilityApp/keys-backup/`. Losing the keystore means future builds can't upgrade
the installed app in place.

## Installing on the Pixel

1. Phone: Settings → Developer options → Wireless debugging → on.
2. First time: tap "Pair device with pairing code", then `scripts/adb-connect.sh pair <ip>:<pairing-port>`.
3. Every session: `scripts/adb-connect.sh <ip>:<port>` (the port shown on the Wireless debugging page).
4. `scripts/deploy.sh release` builds, installs, and also drops the APK in
   `/mnt/c/Users/nghho/accountabilityApp/apks/` for sideloading if adb isn't connected.

No adb? Plug the phone in over USB, choose "File transfer", and run `scripts/push-to-phone.sh`; it copies the
newest APK into the phone's Download folder. Then open it from the Files app to install.
