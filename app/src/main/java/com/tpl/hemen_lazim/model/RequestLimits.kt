package com.tpl.hemen_lazim.model

/**
 * Talep sınırları; sunucudaki model/RequestLimits.java ile aynı tutulmalı.
 * Kaydırıcılar bu aralığın dışına çıkamıyor, sunucu da dışını reddediyor.
 */
object RequestLimits {
    const val MIN_RADIUS_KM = 0.1f
    const val MAX_RADIUS_KM = 10f
    const val DEFAULT_REQUEST_RADIUS_KM = 1.5f
    const val DEFAULT_SEARCH_RADIUS_KM = 0.5f
}
