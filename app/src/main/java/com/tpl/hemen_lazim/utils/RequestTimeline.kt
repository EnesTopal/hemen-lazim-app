package com.tpl.hemen_lazim.utils

import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.RequestLimits
import com.tpl.hemen_lazim.model.enums.RequestStatus
import java.time.Duration
import java.time.Instant

/** İlanlarım'da gösterilen durum. Sunucunun süre işi dakikada bir çalıştığı için arada OPEN
 *  görünen ama süresi geçmiş talep de "süresi doldu" sayılır. */
enum class DisplayStatus { OPEN, EXPIRED, COMPLETED, CANCELLED, MATCHED }

fun MaterialRequestDTO.displayStatus(now: Instant = Instant.now()): DisplayStatus = when (status) {
    RequestStatus.OPEN -> {
        val end = expiresAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (end == null || end.isAfter(now)) DisplayStatus.OPEN else DisplayStatus.EXPIRED
    }
    RequestStatus.EXPIRED -> DisplayStatus.EXPIRED
    RequestStatus.COMPLETED -> DisplayStatus.COMPLETED
    RequestStatus.CANCELLED -> DisplayStatus.CANCELLED
    RequestStatus.MATCHED -> DisplayStatus.MATCHED
}

/**
 * Süre uzatınca sunucunun hesaplayacağı yeni bitiş (MaterialRequestService.extend ile aynı):
 * eski bitiş (geçmişse şimdi) + süre, en fazla şimdiden itibaren en uzun süre (24 sa).
 */
fun extendedEnd(expiresAt: String?, minutes: Int, now: Instant = Instant.now()): Instant {
    val end = expiresAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    val from = if (end == null || end.isBefore(now)) now else end
    val latest = now.plus(Duration.ofMinutes(RequestLimits.MAX_DURATION_MIN.toLong()))
    val extended = from.plus(Duration.ofMinutes(minutes.toLong()))
    return if (extended.isAfter(latest)) latest else extended
}
