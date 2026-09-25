package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.ComebackQuest
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.domain.model.Milestone
import com.nhowe.ember.domain.model.StreakState
import java.time.LocalDate
import java.util.SortedMap

/**
 * Derives the streak from day scores. Nothing is stored: retro-editing a past day simply
 * changes the answer. Rules:
 *  - a day with a score >= threshold extends the streak;
 *  - a rest day (no goals scheduled) is neutral;
 *  - a missed day consumes a streak freeze if one is held, else resets the streak;
 *  - today never breaks the streak; it is "pending" until it is secured or the day rolls over;
 *  - reaching a milestone of 7+ days grants a streak freeze (max 2 held);
 *  - breaking a streak of 3+ days opens a comeback quest: 3 hits in a row earn a freeze back.
 */
object StreakEngine {

    val MILESTONES: List<Int> = listOf(3, 7, 14, 30, 50, 100, 150, 200, 365, 500, 730, 1000)
    const val MAX_FREEZES = 2
    const val FREEZE_MIN_MILESTONE = 7
    const val QUEST_MIN_STREAK = 3
    const val QUEST_TARGET = 3
    private const val EPS = 1e-9

    data class Result(
        val state: StreakState,
        val dayStates: Map<LocalDate, DayState>,
        /** Streak length as of the end of each day (after that day was applied). */
        val streakByDay: Map<LocalDate, Int>,
    )

    fun compute(scores: SortedMap<LocalDate, Double?>, today: LocalDate, threshold: Double): Result {
        var current = 0
        var best = 0
        var bestEnd: LocalDate? = null
        var freezes = 0
        var todaySecured = false
        val milestones = ArrayList<Milestone>()
        val frozen = LinkedHashSet<LocalDate>()
        val dayStates = LinkedHashMap<LocalDate, DayState>()
        val streakByDay = LinkedHashMap<LocalDate, Int>()
        var quest: ComebackQuest? = null
        val completedQuests = ArrayList<ComebackQuest>()

        for ((date, score) in scores) {
            if (date > today) {
                dayStates[date] = DayState.FUTURE
                continue
            }
            val isToday = date == today
            val state: DayState = when {
                score == null -> DayState.REST
                score + EPS >= threshold -> {
                    current++
                    if (current in MILESTONES) {
                        milestones += Milestone(current, date)
                        if (current >= FREEZE_MIN_MILESTONE && freezes < MAX_FREEZES) freezes++
                    }
                    if (isToday) todaySecured = true
                    quest?.let { q ->
                        val next = q.copy(progress = q.progress + 1)
                        if (next.progress >= next.target) {
                            val grant = freezes < MAX_FREEZES
                            if (grant) freezes++
                            completedQuests += next.copy(completedOn = date, freezeGranted = grant)
                            quest = null
                        } else quest = next
                    }
                    if (score >= DayPlan.PERFECT_SCORE) DayState.PERFECT else DayState.HIT
                }
                isToday -> DayState.PENDING
                freezes > 0 -> {
                    freezes--
                    frozen += date
                    DayState.FROZEN
                }
                else -> {
                    quest = when {
                        current >= QUEST_MIN_STREAK -> ComebackQuest(startedOn = date.plusDays(1), brokenStreak = current, target = QUEST_TARGET)
                        else -> quest?.copy(progress = 0)
                    }
                    current = 0
                    DayState.MISS
                }
            }
            if (current > best) {
                best = current
                bestEnd = date
            }
            dayStates[date] = state
            streakByDay[date] = current
        }

        return Result(
            state = StreakState(
                current = current,
                best = best,
                bestEndedOn = bestEnd,
                freezesHeld = freezes,
                milestones = milestones,
                todaySecured = todaySecured,
                frozenDays = frozen,
                quest = quest,
                completedQuests = completedQuests,
            ),
            dayStates = dayStates,
            streakByDay = streakByDay,
        )
    }
}
