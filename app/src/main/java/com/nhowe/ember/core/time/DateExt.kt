package com.nhowe.ember.core.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

const val ALL_WEEKDAYS: Int = 0b1111111

fun LocalDate.toEpochDayInt(): Int = toEpochDay().toInt()

fun Int.toLocalDate(): LocalDate = LocalDate.ofEpochDay(toLong())

/** Bit for this date's weekday: bit 0 = Monday … bit 6 = Sunday. */
fun LocalDate.weekdayBit(): Int = 1 shl (dayOfWeek.value - 1)

fun DayOfWeek.bit(): Int = 1 shl (value - 1)

fun Int.hasWeekday(day: DayOfWeek): Boolean = (this and day.bit()) != 0

fun LocalDate.startOfWeek(): LocalDate = minusDays((dayOfWeek.value - 1).toLong())

fun LocalDate.endOfWeek(): LocalDate = startOfWeek().plusDays(6)

fun LocalDate.startOfMonth(): LocalDate = withDayOfMonth(1)

fun LocalDate.endOfMonth(): LocalDate = YearMonth.from(this).atEndOfMonth()

/** Inclusive date range as a sequence. Empty if [to] is before [from]. */
fun datesBetween(from: LocalDate, to: LocalDate): Sequence<LocalDate> =
    generateSequence(from) { d -> if (d < to) d.plusDays(1) else null }
        .takeWhile { it <= to }
