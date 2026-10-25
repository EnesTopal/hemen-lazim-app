package com.tpl.hemen_lazim.session

import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

object SessionManager {

    /**
     * Cihazda kullanılabilir bir oturum var mı? Yalnızca yerel bilgiye bakar, ağa çıkmaz; açılış
     * ekranı bu yüzden bekletmez. Refresh token varsa erişim süresi dolmuş olsa da oturum vardır:
     * ilk istek 401 alınca TokenAuthenticator yeniler, yenileyemezse [SessionEvents.expired]
     * yayılır ve kullanıcı girişe döner.
     */
    fun hasSession(): Boolean {
        if (!SharedPreferencesProvider.getRefreshToken().isNullOrBlank()) return true
        val access = SharedPreferencesProvider.getAccessToken()
        val exp = SharedPreferencesProvider.accessTokenExpSeconds()
        return !access.isNullOrBlank() && exp != null && System.currentTimeMillis() / 1000 < exp
    }
}
