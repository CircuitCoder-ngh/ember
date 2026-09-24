package com.nhowe.ember.domain.engine

import com.nhowe.ember.data.backup.BackupCodec
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.Settings
import org.junit.Test
import kotlin.test.assertEquals

class BackupCodecTest {
    @Test
    fun `export then import round trips`() {
        val h = history {
            goal("a", GoalType.QUANTITY, target = 20, to = d(10), title = "Read 20")
            goal("a", GoalType.QUANTITY, target = 40, from = d(11), title = "Read 40")
            goal("gym", target = 3, cadence = Cadence.WEEKLY)
            done("a", d(2)); count("a", d(12), 20); skip("a", d(3)); done("gym", d(2))
        }
        val s = Settings(streakThreshold = 0.9, dayBoundaryHour = 3, userName = "N")
        val text = BackupCodec.encode(h, s, "2026-09-24T00:00:00Z")
        val (h2, s2) = BackupCodec.decode(text)
        assertEquals(h, h2)
        assertEquals(s.copy(onboardingDone = true), s2)
    }
}
