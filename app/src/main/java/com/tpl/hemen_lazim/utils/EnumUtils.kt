package com.tpl.hemen_lazim.utils

import java.util.Locale

object EnumUtils {
    // modern, deprecated olmayan Locale elde edişi
    private val TURKISH_LOCALE: Locale = Locale.forLanguageTag("tr-TR")
    // alternatif: Locale.Builder().setLanguage("tr").setRegion("TR").build()

    /**
     * Örnek:
     *  "YEMEK_MALZEMESI" -> "Yemek malzemesi"
     *  "KIRTASIYE"       -> "Kırtasiye"
     */
    fun formatEnumDisplayName(enumName: String): String {
        return enumName
            .split("_")
            .joinToString(" ") { part ->
                if (part.isEmpty()) return@joinToString part

                // önce tamamen Türkçe locale ile küçült
                val lower = part.lowercase(TURKISH_LOCALE)

                // sonra ilk harfi Türkçe locale ile büyüt, geri kalanını ekle
                val first = lower.substring(0, 1).uppercase(TURKISH_LOCALE)
                val rest = if (lower.length > 1) lower.substring(1) else ""

                first + rest
            }
    }
}
