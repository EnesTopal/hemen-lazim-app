package com.tpl.hemen_lazim.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class FormattersTest {

    private val turkish = Locale.forLanguageTag("tr-TR")

    @Test
    fun distanceIsInMetresBelowOneKilometre() {
        assertEquals("850 m", formatDistance(847.0, turkish))
        assertEquals("1,0 km", formatDistance(998.0, turkish))
        assertEquals("1,2 km", formatDistance(1_234.0, turkish))
    }

    @Test
    fun distanceUsesTheDecimalSeparatorOfTheLanguage() {
        assertEquals("1.2 km", formatDistance(1_234.0, Locale.ENGLISH))
    }

    @Test
    fun durationIsSplitIntoDaysHoursAndMinutes() {
        assertEquals(DurationParts(0, 0, 45), splitDuration(45))
        assertEquals(DurationParts(0, 2, 15), splitDuration(135))
        assertEquals(DurationParts(0, 3, 0), splitDuration(180))
        assertEquals(DurationParts(1, 0, 0), splitDuration(24 * 60))
    }

    @Test
    fun remainingTimeCoversRunningAlmostDueAndPastRequests() {
        val now = Instant.parse("2026-10-24T13:30:00Z")

        assertEquals(Remaining.Minutes(135), remainingUntil("2026-10-24T15:45:00Z", now))
        assertEquals(Remaining.UnderAMinute, remainingUntil("2026-10-24T13:30:30Z", now))
        assertEquals(Remaining.Expired, remainingUntil("2026-10-24T13:29:00Z", now))
        assertNull(remainingUntil("not-a-date", now))
    }

    @Test
    fun relativeDayNamesTheNeighbouringDaysAndSplitsByYear() {
        val today = LocalDate.of(2026, 10, 25)

        assertEquals(RelativeDay.TODAY, relativeDay(today, today))
        assertEquals(RelativeDay.TOMORROW, relativeDay(LocalDate.of(2026, 10, 26), today))
        assertEquals(RelativeDay.YESTERDAY, relativeDay(LocalDate.of(2026, 10, 24), today))
        assertEquals(RelativeDay.THIS_YEAR, relativeDay(LocalDate.of(2026, 1, 3), today))
        assertEquals(RelativeDay.OTHER_YEAR, relativeDay(LocalDate.of(2025, 10, 23), today))
    }

    @Test
    fun newYearsEveIsYesterdayNotLastYear() {
        assertEquals(RelativeDay.YESTERDAY, relativeDay(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)))
    }
}
