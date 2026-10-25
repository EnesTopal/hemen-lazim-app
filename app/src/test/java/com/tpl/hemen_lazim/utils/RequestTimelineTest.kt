package com.tpl.hemen_lazim.utils

import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.RequestStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RequestTimelineTest {

    private val now = Instant.parse("2026-10-25T12:00:00Z")

    @Test
    fun openRequestPastItsEndIsShownAsExpired() {
        // Sunucunun dakikalık işi henüz EXPIRED yapmamış olabilir
        assertEquals(DisplayStatus.EXPIRED, request(RequestStatus.OPEN, now.minusSeconds(1)).displayStatus(now))
        assertEquals(DisplayStatus.OPEN, request(RequestStatus.OPEN, now.plusSeconds(60)).displayStatus(now))
        assertEquals(DisplayStatus.OPEN, request(RequestStatus.OPEN, null).displayStatus(now))
        assertEquals(DisplayStatus.CANCELLED, request(RequestStatus.CANCELLED, now.plusSeconds(60)).displayStatus(now))
    }

    @Test
    fun extendingAddsToTheCurrentEnd() {
        val end = now.plus(Duration.ofMinutes(30))
        assertEquals(end.plus(Duration.ofMinutes(60)), extendedEnd(end.toString(), 60, now))
    }

    @Test
    fun extendingAPassedEndCountsFromNow() {
        assertEquals(now.plus(Duration.ofMinutes(60)), extendedEnd(now.minusSeconds(300).toString(), 60, now))
    }

    @Test
    fun extendingNeverGoesBeyondTwentyFourHoursFromNow() {
        val end = now.plus(Duration.ofHours(23))
        assertEquals(now.plus(Duration.ofHours(24)), extendedEnd(end.toString(), 120, now))
    }

    private fun request(status: RequestStatus, expiresAt: Instant?) = MaterialRequestDTO(
        id = "r1",
        requesterId = "u1",
        requesterName = "ayse",
        title = "un",
        description = null,
        category = Category.YEMEK_MALZEMESI,
        quantity = 2,
        units = null,
        latitude = 41.0,
        longitude = 29.0,
        radiusMeters = 1000,
        status = status,
        expiresAt = expiresAt?.toString(),
        createdAt = now.toString()
    )
}
