# Ember: program templates (multi-week plans people already follow)

Brainstorm, 2026-09-25. Program details are from the widely published versions of each plan, from memory
and not re-checked against sources; verify the specifics before shipping any of them.

## The big idea: "Programs", not just templates

A **template** fills in goals once. A **program** is a template with a calendar: it changes the goals week
by week, has a finish line, and ends with a graduation badge. Most plans people follow for weeks or months
are the second kind (Couch to 5K gets harder every week; a Bible-in-a-year plan has a different reading
each day).

Ember's data model already suits this well. `GoalVersion` has `validFrom` / `validTo`
(`domain/model/Goal.kt`), so a program can be written as a chain of **pre-dated versions**: week 1's
version runs days 1–7, week 2's version starts on day 8, and so on. History, scoring and the calendar would
show each week's target correctly without any engine changes. (Inferred from the model. I haven't checked
whether the editor's "close yesterday, open today" rule copes with versions dated in the future; it
probably needs a guard.)

What a program would add on top:
- **Start date + length**, with a "Week 3 of 9" label on the Today screen and a progress bar.
- **Phase changes** (new target, new title such as "Run 3 × 5 min") that happen automatically.
- **Graduation**: a badge, a big XP award, a confetti moment, and "What's next?" (e.g. 5K → 10K).
- **Pause / restart week** for illness or travel, so people don't abandon the whole plan.
- A few **program-specific streak rules** (e.g. "strict" programs where a miss restarts at day 1).

## Fitness

| Program | Length | How it maps to Ember |
|---|---|---|
| **Couch to 5K** style run plan | 9 weeks | Weekly goal "Run 3×"; each week's version updates the interval note (week 1: 60s run / 90s walk × 8 → week 9: 30 min continuous). Graduates into a 10K plan. |
| **Beginner 10K / half marathon** (Hal Higdon novice style) | 8–12 weeks | Daily goals on set weekdays (short runs, one long run on the weekend, cross-train, rest); long-run distance ramps weekly. |
| **100 push-ups** style plan | 6 weeks | Daily count-up goal, 3 days/week; target ramps each week. Starts with a test to set the level. |
| **30-day squat / plank challenge** | 30 days | Daily count-up where the target climbs every day or two, with rest days built in. |
| **StrongLifts 5×5 / Starting Strength** style | Open-ended, ~12 weeks | Weekly goal "Lift 3×" alternating workouts A/B; a note field for weights. Ramp is in the weights, which Ember doesn't track, so this is a lighter fit. |
| **30 days of yoga** (Yoga with Adriene style) | 30 days | Daily check "Yoga (day N)", link or note per day. |
| **10,000 steps** | Ongoing | Daily count-up; ideal for Health Connect auto-fill later. |

## Mind and wellbeing

| Program | Length | How it maps to Ember |
|---|---|---|
| **21-day meditation** (the common app format) | 21 days | Daily count-up in minutes, ramping 5 → 10 → 15. |
| **The Artist's Way** | 12 weeks | Daily "Morning pages (3 pages)", weekly "Artist date", weekly chapter + tasks. A very loyal audience. |
| **Miracle Morning (SAVERS)** | 30-day start | Six small daily checks: Silence, Affirmations, Visualisation, Exercise, Reading, Scribing. |
| **5-minute journal / gratitude** | Ongoing | Morning "3 gratitudes", evening "3 good things". |
| **Digital detox** | 7–30 days | Daily "Screen time under X", target tightening each week; "No phone first hour". |
| **Sleep reset** | 2–4 weeks | Daily "In bed by 23:00" with the time moving 15 min earlier each week, "No caffeine after 14:00", morning daylight. |

## Learning and creative

| Program | Length | How it maps to Ember |
|---|---|---|
| **#100DaysOfCode** | 100 days | Daily "Code 1 hour"; the 100-day finish line suits graduation. |
| **NaNoWriMo-style novel month** | November (30 days) | Daily count-up of 1,667 words; monthly goal 50,000 words. |
| **Read 52 books a year** | 1 year | Weekly goal "Finish a book" or daily "Read 20 pages". |
| **Language learning** | Ongoing / 90 days | Daily "Flashcards (Anki) 15 min", weekly "1 conversation", monthly "1 short story". |
| **Learn an instrument** | 8–12 weeks | Daily practice minutes ramping, weekly "Learn one new piece". |

## Faith and seasonal

| Program | Length | How it maps to Ember |
|---|---|---|
| **Bible in a year** | 365 days | Daily reading with a different passage in the note each day (a program with 365 phases). |
| **Quran in Ramadan** | ~30 days | Daily "1 juz"; tied to the lunar calendar dates each year. |
| **Lent** | 40 days | A daily "give up X" check plus a daily prayer/reading. |
| **Dry January / Sober October** | 1 month | Daily "Alcohol-free" check; natural fit for streaks. Monthly seasonal launch = marketing moment. |
| **New Year reset** | January | Bundle: a few daily habits, weekly review, monthly goal. |

## Money and home

| Program | Length | How it maps to Ember |
|---|---|---|
| **52-week savings challenge** | 1 year | Weekly goal "Save £N" where N = week number (1, 2, 3 … 52 → about 1,378 total). A textbook ramping program. |
| **No-spend month** | 30 days | Daily "No non-essential spending" check. |
| **Minimalism game** | 30 days | Day N: get rid of N items (a daily count-up whose target is the day number). |
| **Declutter by room** | 4–8 weeks | Weekly one-off focus "Kitchen", "Wardrobe", … with small daily 15-minute tidies. |

## Discipline challenges

| Program | Length | How it maps to Ember |
|---|---|---|
| **"75 Hard" style** | 75 days | Five daily checks (two workouts, diet, water, 10 pages); **strict** rule: a miss restarts at day 1. Very popular on social media. The name is trademarked, so ship a generic "75-day challenge" and a gentler "soft" version. |
| **Atomic Habits / Tiny Habits starter** | 4 weeks | One tiny daily habit in week 1, adding one per week; teaches habit stacking. |

## Onboarding ideas that use these

- **"What do you want to work on?"** picker (Fitness, Mind, Learning, Money, Faith, Creative) that
  suggests two or three programs plus a few everyday habits.
- **Level check** for fitness plans ("Can you run 5 min?") that starts people at the right week.
- **Stacking guard**: warn when someone starts three programs at once. Recommend one program + a couple of
  habits, because overloading on day one is a classic reason people drop off.
- **Seasonal shelf**: surface Dry January, NaNoWriMo, Ramadan, Lent, New Year when they come round.
  This gives the app a reason to reappear each season.

## Later

- **Custom program builder** and **share a program as a file** (the JSON backup codec is a good starting
  point), so coaches, teachers or friends can hand a plan to someone.
- **Program library updates** without an app release, by shipping programs as data files.
