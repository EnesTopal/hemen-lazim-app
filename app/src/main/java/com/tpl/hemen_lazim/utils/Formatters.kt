package com.tpl.hemen_lazim.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

// Dile bağlı kelimeler (dk, bugün, Dün...) burada yok: ekranda ui/TimeTexts.kt strings.xml'den
// kuruyor. Buradaki işlevler yalnızca hesaplıyor ve testleri JVM'de çalışıyor.

private val TURKISH: Locale = Locale.forLanguageTag("tr-TR")

/**
 * "850 m" / "1,2 km" / "10 km": kilometrede tek ondalık, tam kilometrede ondalıksız. Ondalık
 * ayırıcı dile göre (İngilizcede "1.2 km").
 */
fun formatDistance(meters: Double, locale: Locale = Locale.getDefault()): String {
    val roundedMeters = (meters / 10).roundToInt() * 10
    if (roundedMeters < 1000) return "$roundedMeters m"
    val tenthsOfKm = (meters / 100).roundToInt()
    return if (tenthsOfKm % 10 == 0) String.format(locale, "%d km", tenthsOfKm / 10)
    else String.format(locale, "%.1f km", tenthsOfKm / 10.0)
}

fun formatRadiusKm(km: Float, locale: Locale = Locale.getDefault()): String = formatDistance(km * 1000.0, locale)

data class DurationParts(val days: Int, val hours: Int, val minutes: Int)

fun splitDuration(totalMinutes: Int): DurationParts = DurationParts(
    days = totalMinutes / (24 * 60),
    hours = totalMinutes % (24 * 60) / 60,
    minutes = totalMinutes % 60
)

/** Bitişe kalan süre. */
sealed interface Remaining {
    data object Expired : Remaining
    data object UnderAMinute : Remaining
    data class Minutes(val minutes: Int) : Remaining
}

/** Sunucunun ISO-8601 bitiş anına kalan süre; okunamazsa null. */
fun remainingUntil(expiresAt: String, now: Instant = Instant.now()): Remaining? {
    val left = runCatching { Duration.between(now, Instant.parse(expiresAt)) }.getOrNull() ?: return null
    return when {
        left.isNegative -> Remaining.Expired
        left.toMinutes() < 1 -> Remaining.UnderAMinute
        else -> Remaining.Minutes(left.toMinutes().toInt())
    }
}

enum class RelativeDay { TODAY, TOMORROW, YESTERDAY, THIS_YEAR, OTHER_YEAR }

fun relativeDay(date: LocalDate, today: LocalDate): RelativeDay = when (date) {
    today -> RelativeDay.TODAY
    today.plusDays(1) -> RelativeDay.TOMORROW
    today.minusDays(1) -> RelativeDay.YESTERDAY
    else -> if (date.year == today.year) RelativeDay.THIS_YEAR else RelativeDay.OTHER_YEAR
}

/**
 * Avatar için baş harfler: "ayşe yılmaz" → "AY", "cem" → "C". İsimlerin çoğu Türkçe olduğu
 * için telefonun dilinden bağımsız Türkçe büyük harf ("i" → "İ").
 */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(TURKISH) }
        .ifEmpty { "?" }
