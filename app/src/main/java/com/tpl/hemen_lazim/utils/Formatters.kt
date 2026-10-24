package com.tpl.hemen_lazim.utils

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private val TURKISH: Locale = Locale.forLanguageTag("tr-TR")

/** "850 m" / "1,2 km" */
fun formatDistance(meters: Double): String {
    val roundedMeters = (meters / 10).roundToInt() * 10
    return if (roundedMeters < 1000) "$roundedMeters m"
    else String.format(TURKISH, "%.1f km", meters / 1000)
}

fun formatRadiusKm(km: Float): String = formatDistance(km * 1000.0)

/** "45 dk" / "2 sa 15 dk" / "1 gün" */
fun formatDuration(minutes: Int): String {
    val days = minutes / (24 * 60)
    val hours = minutes % (24 * 60) / 60
    val mins = minutes % 60
    return listOfNotNull(
        days.takeIf { it > 0 }?.let { "$it gün" },
        hours.takeIf { it > 0 }?.let { "$it sa" },
        mins.takeIf { it > 0 }?.let { "$it dk" }
    ).joinToString(" ").ifEmpty { "0 dk" }
}

/** Şimdiden [minutes] sonrası: "bugün 18:45" / "yarın 09:30" / "26 Ekim 10:00" */
fun formatEndTime(minutes: Int, now: ZonedDateTime = ZonedDateTime.now()): String {
    val end = now.plusMinutes(minutes.toLong())
    val day = when (ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate())) {
        0L -> "bugün"
        1L -> "yarın"
        else -> end.format(DateTimeFormatter.ofPattern("d MMMM", TURKISH))
    }
    return "$day ${end.format(DateTimeFormatter.ofPattern("HH:mm", TURKISH))}"
}

/** Sunucunun ISO-8601 bitiş anına kalan süre; okunamazsa null. */
fun formatRemaining(expiresAt: String, now: Instant = Instant.now()): String? {
    val left = runCatching { Duration.between(now, Instant.parse(expiresAt)) }.getOrNull() ?: return null
    return when {
        left.isNegative -> "Süresi doldu"
        left.toMinutes() < 1 -> "1 dk'dan az"
        else -> formatDuration(left.toMinutes().toInt())
    }
}
