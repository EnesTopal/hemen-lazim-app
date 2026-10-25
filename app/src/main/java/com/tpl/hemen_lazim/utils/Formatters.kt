package com.tpl.hemen_lazim.utils

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
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

/** Mesaj saati: "14:05" */
fun formatClock(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", TURKISH))

/** Sohbet listesindeki son mesaj zamanı: bugünse "14:05", dünse "Dün", bu yıl "23 Eki", yoksa "23.10.2025". */
fun formatChatListTime(epochMillis: Long, now: ZonedDateTime = ZonedDateTime.now()): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(now.zone)
    return when {
        time.toLocalDate() == now.toLocalDate() -> time.format(DateTimeFormatter.ofPattern("HH:mm", TURKISH))
        time.toLocalDate() == now.toLocalDate().minusDays(1) -> "Dün"
        time.year == now.year -> time.format(DateTimeFormatter.ofPattern("d MMM", TURKISH))
        else -> time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", TURKISH))
    }
}

/** Sohbetteki gün ayracı: "Bugün", "Dün", "23 Ekim" ya da başka yılsa "23 Ekim 2025". */
fun formatDayLabel(epochMillis: Long, now: ZonedDateTime = ZonedDateTime.now()): String {
    val day = Instant.ofEpochMilli(epochMillis).atZone(now.zone).toLocalDate()
    return when {
        day == now.toLocalDate() -> "Bugün"
        day == now.toLocalDate().minusDays(1) -> "Dün"
        day.year == now.year -> day.format(DateTimeFormatter.ofPattern("d MMMM", TURKISH))
        else -> day.format(DateTimeFormatter.ofPattern("d MMMM yyyy", TURKISH))
    }
}

/** Avatar için baş harfler: "ayşe yılmaz" → "AY", "cem" → "C". Türkçe büyük harf ("i" → "İ"). */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(TURKISH) }
        .ifEmpty { "?" }
