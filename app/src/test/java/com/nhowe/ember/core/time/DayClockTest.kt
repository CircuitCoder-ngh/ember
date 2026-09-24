package com.nhowe.ember.core.time

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test
import kotlin.test.assertEquals

class DayClockTest {

    @Test
    fun `boundary hour shifts late nights to the previous day`() {
        val d24 = LocalDate.of(2026, 9, 24)
        assertEquals(d24.minusDays(1), DayClock.logicalDate(LocalDateTime.of(2026, 9, 24, 2, 59), boundaryHour = 3))
        assertEquals(d24, DayClock.logicalDate(LocalDateTime.of(2026, 9, 24, 3, 0), boundaryHour = 3))
        assertEquals(d24, DayClock.logicalDate(LocalDateTime.of(2026, 9, 24, 0, 0), boundaryHour = 0))
    }
}
