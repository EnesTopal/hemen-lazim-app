package com.tpl.hemen_lazim.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.widget.Toast
import org.json.JSONObject

object SharedPreferencesProvider {
    private var sharedPreferences: SharedPreferences? = null
    private const val PREF_NAME = "MyToken"
    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val FCM_TOKEN_KEY = "fcm_token"

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getPreferences(): SharedPreferences =
        sharedPreferences ?: throw IllegalStateException("SharedPreferences not initialized. Call init() first.")


    fun saveSession(accessToken: String, refreshToken: String) {
        sharedPreferences?.edit()
            ?.putString(ACCESS_TOKEN_KEY, accessToken)
            ?.putString(REFRESH_TOKEN_KEY, refreshToken)
            ?.apply()
    }

    fun clearSession() {
        sharedPreferences?.edit()
            ?.remove(ACCESS_TOKEN_KEY)
            ?.remove(REFRESH_TOKEN_KEY)
            ?.apply()
    }

    fun getAccessToken(): String? = sharedPreferences?.getString(ACCESS_TOKEN_KEY, null)
    fun getRefreshToken(): String? = sharedPreferences?.getString(REFRESH_TOKEN_KEY, null)

    fun saveToken(token: String) = sharedPreferences?.edit()?.putString(ACCESS_TOKEN_KEY, token)?.apply()
    fun getToken(): String? = getAccessToken()
    fun clearToken() = sharedPreferences?.edit()?.remove(ACCESS_TOKEN_KEY)?.apply()
    
    // FCM Token methods
    fun saveFcmToken(token: String) = sharedPreferences?.edit()?.putString(FCM_TOKEN_KEY, token)?.apply()
    fun getFcmToken(): String? = sharedPreferences?.getString(FCM_TOKEN_KEY, null)
    fun clearFcmToken() = sharedPreferences?.edit()?.remove(FCM_TOKEN_KEY)?.apply()


    fun accessTokenExpSeconds(): Long? = getAccessToken()?.let { getTokenExpirationTime(it) }

    fun isAccessTokenAboutToExpire(thresholdSeconds: Long = 60L): Boolean {
        val exp = accessTokenExpSeconds() ?: return true
        val now = System.currentTimeMillis() / 1000
        return (exp - now) <= thresholdSeconds
    }

    fun getPreferredUsername(): String? = getAccessToken()?.let { token ->
        try {
            val payload = jwtPayloadJson(token)
            when {
                payload.has("preferred_username") -> payload.getString("preferred_username")
                payload.has("sub") -> payload.getString("sub")
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun jwtPayloadJson(token: String): JSONObject {
        val parts = token.split(".")
        require(parts.size == 3) { "Invalid JWT" }
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
        return JSONObject(payload)
    }

    private fun getTokenExpirationTime(token: String): Long? {
        return try {
            jwtPayloadJson(token).getLong("exp")
        } catch (_: Exception) { null }
    }
    fun isTokenValidWithToast(context: Context): Boolean {
        val token = getAccessToken() ?: return false.also {
            Toast.makeText(context, "Oturum bulunamadı.", Toast.LENGTH_SHORT).show()
        }
        val exp = getTokenExpirationTime(token) ?: return false.also {
            Toast.makeText(context, "Geçersiz oturum.", Toast.LENGTH_SHORT).show()
        }
        val currentTime = System.currentTimeMillis() / 1000
        val isValid = currentTime < exp
        if (!isValid) {
            Toast.makeText(context, "Oturum süresi dolmuş. Lütfen tekrar giriş yapın.", Toast.LENGTH_LONG).show()
        }
        return isValid
    }
}
