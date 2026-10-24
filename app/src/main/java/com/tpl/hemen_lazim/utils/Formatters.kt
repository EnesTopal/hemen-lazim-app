package com.tpl.hemen_lazim.utils

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
