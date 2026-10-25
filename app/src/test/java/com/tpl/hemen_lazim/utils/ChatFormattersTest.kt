package com.tpl.hemen_lazim.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ChatFormattersTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val now = ZonedDateTime.of(2026, 10, 25, 16, 30, 0, 0, zone)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 14, minute: Int = 5) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun chatListShowsClockTodayAndDatesBefore() {
        assertEquals("14:05", formatChatListTime(at(2026, 10, 25), now))
        assertEquals("Dün", formatChatListTime(at(2026, 10, 24), now))
        assertEquals("23 Eki", formatChatListTime(at(2026, 10, 23), now))
        assertEquals("23.10.2025", formatChatListTime(at(2025, 10, 23), now))
    }

    @Test
    fun dayLabelsAreRelativeForTodayAndYesterday() {
        assertEquals("Bugün", formatDayLabel(at(2026, 10, 25), now))
        assertEquals("Dün", formatDayLabel(at(2026, 10, 24), now))
        assertEquals("23 Ekim", formatDayLabel(at(2026, 10, 23), now))
        assertEquals("23 Ekim 2025", formatDayLabel(at(2025, 10, 23), now))
    }

    @Test
    fun clockIsTwentyFourHour() {
        assertEquals("09:07", formatClock(at(2026, 10, 25, 9, 7), zone))
    }

    @Test
    fun initialsUseTurkishCapitals() {
        assertEquals("AY", initialsOf("ayşe yılmaz"))
        assertEquals("İ", initialsOf("ilker"))
        assertEquals("C", initialsOf("  cem  "))
        assertEquals("?", initialsOf(""))
    }
}
