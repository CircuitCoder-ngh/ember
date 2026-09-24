package com.nhowe.ember.core.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * The app's notion of "today". A day boundary hour of 3 means 02:59 still belongs to the
 * previous calendar day ("the day ends at 3 am"), so late-night check-offs count.
 */
class DayClock(
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val now: () -> Instant = { Instant.now() },
) {
    fun today(boundaryHour: Int): LocalDate =
        logicalDate(LocalDateTime.ofInstant(now(), zone()), boundaryHour)

    /** The instant at which the logical day next rolls over. */
    fun nextRollover(boundaryHour: Int): Instant {
        val zoneId = zone()
        val nowLocal = LocalDateTime.ofInstant(now(), zoneId)
        val today = logicalDate(nowLocal, boundaryHour)
        val next = today.plusDays(1).atTime(boundaryHour.coerceIn(0, 23), 0)
        return next.atZone(zoneId).toInstant()
    }

    /** Emits the logical date now and again every time it changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun todayFlow(boundaryHour: Flow<Int>): Flow<LocalDate> =
        boundaryHour.flatMapLatest { hour ->
            flow {
                while (true) {
                    emit(today(hour))
                    val wait = Duration.between(now(), nextRollover(hour)).toMillis().coerceAtLeast(1_000)
                    delay(wait + 500)
                }
            }
        }.distinctUntilChanged()

    companion object {
        fun logicalDate(dateTime: LocalDateTime, boundaryHour: Int): LocalDate =
            dateTime.minusHours(boundaryHour.coerceIn(0, 23).toLong()).toLocalDate()
    }
}
