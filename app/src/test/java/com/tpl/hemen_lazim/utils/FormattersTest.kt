package com.tpl.hemen_lazim.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class FormattersTest {

    @Test
    fun distanceIsInMetresBelowOneKilometre() {
        assertEquals("850 m", formatDistance(847.0))
        assertEquals("1,0 km", formatDistance(998.0))
        assertEquals("1,2 km", formatDistance(1_234.0))
    }

    @Test
    fun durationIsSplitIntoDaysHoursAndMinutes() {
        assertEquals("45 dk", formatDuration(45))
        assertEquals("2 sa 15 dk", formatDuration(135))
        assertEquals("3 sa", formatDuration(180))
        assertEquals("1 gün", formatDuration(24 * 60))
    }

    @Test
    fun endTimeSaysTodayOrTomorrow() {
        val now = ZonedDateTime.of(2026, 10, 24, 16, 30, 0, 0, ZoneId.of("Europe/Istanbul"))

        assertEquals("bugün 18:45", formatEndTime(135, now))
        assertEquals("yarın 09:30", formatEndTime(17 * 60, now))
    }

    @Test
    fun remainingTimeCoversRunningAlmostDueAndPastRequests() {
        val now = Instant.parse("2026-10-24T13:30:00Z")

        assertEquals("2 sa 15 dk", formatRemaining("2026-10-24T15:45:00Z", now))
        assertEquals("1 dk'dan az", formatRemaining("2026-10-24T13:30:30Z", now))
        assertEquals("Süresi doldu", formatRemaining("2026-10-24T13:29:00Z", now))
        assertNull(formatRemaining("not-a-date", now))
    }
}
