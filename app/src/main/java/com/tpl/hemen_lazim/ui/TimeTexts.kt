package com.tpl.hemen_lazim.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.utils.RelativeDay
import com.tpl.hemen_lazim.utils.Remaining
import com.tpl.hemen_lazim.utils.formatDistance
import com.tpl.hemen_lazim.utils.formatRadiusKm
import com.tpl.hemen_lazim.utils.relativeDay
import com.tpl.hemen_lazim.utils.remainingUntil
import com.tpl.hemen_lazim.utils.splitDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale

// Ekrandaki süre, tarih ve uzaklık metinleri: kelimeler strings.xml'den, sayı ve tarih biçimi
// metinlerin dilinden (contentLocale). Hesaplar utils/Formatters.kt'de (testli).

/**
 * Metinlerin geldiği dil. Telefonun dili destekleniyorsa onun bölgesiyle (en-GB, en-US), değilse
 * varsayılan metinlerin dili: Almanca bir telefonda İngilizce metnin yanında Almanca tarih
 * çıkmasın.
 */
@Composable
@ReadOnlyComposable
fun contentLocale(): Locale {
    val device = LocalConfiguration.current.locales[0]
    val content = Locale.forLanguageTag(stringResource(R.string.content_locale))
    return if (device.language == content.language) device else content
}

/** "850 m" / "1,2 km" */
@Composable
@ReadOnlyComposable
fun distanceText(meters: Double): String = formatDistance(meters, contentLocale())

@Composable
@ReadOnlyComposable
fun radiusText(km: Float): String = formatRadiusKm(km, contentLocale())

/** "45 dk" / "2 sa 15 dk" / "1 gün" */
@Composable
@ReadOnlyComposable
fun durationText(minutes: Int): String {
    val (days, hours, mins) = splitDuration(minutes)
    val parts = mutableListOf<String>()
    if (days > 0) parts += pluralStringResource(R.plurals.duration_days, days, days)
    if (hours > 0) parts += stringResource(R.string.duration_hours, hours)
    if (mins > 0 || parts.isEmpty()) parts += stringResource(R.string.duration_minutes, mins)
    return parts.joinToString(" ")
}

/** Bitişe kalan süre ("2 sa 15 dk", "1 dk'dan az"); süresi geçtiyse ya da okunamazsa null. */
@Composable
@ReadOnlyComposable
fun remainingText(expiresAt: String, now: Instant = Instant.now()): String? =
    when (val left = remainingUntil(expiresAt, now)) {
        null, Remaining.Expired -> null
        Remaining.UnderAMinute -> stringResource(R.string.remaining_under_a_minute)
        is Remaining.Minutes -> durationText(left.minutes)
    }

/** Saat, telefonun 12/24 saat ayarına göre: "14:05" ya da "2:05 PM". */
@Composable
@ReadOnlyComposable
fun clockText(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    clockText(Instant.ofEpochMilli(epochMillis).atZone(zone))

@Composable
@ReadOnlyComposable
fun clockText(time: ZonedDateTime): String {
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm"
    return time.formatSkeleton(skeleton, contentLocale())
}

/** Şimdiden [minutes] sonrası: "bugün 18:45" / "yarın 09:30" / "26 Ekim 10:00" */
@Composable
@ReadOnlyComposable
fun endTimeText(minutes: Int, now: ZonedDateTime = ZonedDateTime.now()): String {
    val end = now.plusMinutes(minutes.toLong())
    val time = clockText(end)
    return when (relativeDay(end.toLocalDate(), now.toLocalDate())) {
        RelativeDay.TODAY -> stringResource(R.string.end_today, time)
        RelativeDay.TOMORROW -> stringResource(R.string.end_tomorrow, time)
        else -> stringResource(R.string.end_on_date, end.formatSkeleton("MMMMd", contentLocale()), time)
    }
}

/** Sohbet listesindeki son mesaj zamanı: bugünse saat, dünse "Dün", bu yıl "23 Eki", yoksa kısa tarih. */
@Composable
@ReadOnlyComposable
fun chatListTimeText(epochMillis: Long, now: ZonedDateTime = ZonedDateTime.now()): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(now.zone)
    val locale = contentLocale()
    return when (relativeDay(time.toLocalDate(), now.toLocalDate())) {
        RelativeDay.TODAY -> clockText(time)
        RelativeDay.YESTERDAY -> stringResource(R.string.day_yesterday)
        RelativeDay.TOMORROW, RelativeDay.THIS_YEAR -> time.formatSkeleton("MMMd", locale)
        RelativeDay.OTHER_YEAR -> time.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale))
    }
}

/** Sohbetteki gün ayracı: "Bugün", "Dün", "23 Ekim" ya da "23 Ekim 2025". */
@Composable
@ReadOnlyComposable
fun dayLabelText(day: LocalDate, today: LocalDate = LocalDate.now()): String {
    val locale = contentLocale()
    return when (relativeDay(day, today)) {
        RelativeDay.TODAY -> stringResource(R.string.day_today)
        RelativeDay.YESTERDAY -> stringResource(R.string.day_yesterday)
        RelativeDay.TOMORROW, RelativeDay.THIS_YEAR -> day.formatSkeleton("MMMMd", locale)
        RelativeDay.OTHER_YEAR -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    }
}

// Alanların sırası ve ayraçlar dile göre: "26 Ekim" / "October 26" / "26 October" (en-GB)
private fun TemporalAccessor.formatSkeleton(skeleton: String, locale: Locale): String =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(this)
