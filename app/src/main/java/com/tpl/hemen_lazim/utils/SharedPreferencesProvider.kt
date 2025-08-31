package com.tpl.hemen_lazim.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.widget.Toast
import org.json.JSONObject


object SharedPreferencesProvider {
    private var sharedPreferences: SharedPreferences? = null
    private const val TOKEN_KEY = "auth_token"

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("MyToken", Context.MODE_PRIVATE)
        }
    }

    fun getPreferences(): SharedPreferences {
        return sharedPreferences
            ?: throw IllegalStateException("SharedPreferences not initialized. Call init() first.")
    }

    fun saveToken(token: String) {
        sharedPreferences?.edit()?.putString(TOKEN_KEY, token)?.apply()
    }

    fun getToken(): String? {
        return sharedPreferences?.getString(TOKEN_KEY, null)
    }

    fun clearToken() {
        sharedPreferences?.edit()?.remove(TOKEN_KEY)?.apply()
    }

    fun isTokenValidWithToast(context: Context): Boolean {
        val token = getToken() ?: return false.also {
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


    private fun getTokenExpirationTime(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            json.getLong("exp")
        } catch (e: Exception) {
            null
        }
    }
}
