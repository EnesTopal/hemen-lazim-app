package com.tpl.hemen_lazim.network.repositories

import android.content.Context
import android.util.Log
import com.tpl.hemen_lazim.model.DTOs.*
import com.tpl.hemen_lazim.network.services.AuthService
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.network.apiResult
import com.tpl.hemen_lazim.network.bodyOrThrow
import com.tpl.hemen_lazim.network.toApiException
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import com.tpl.hemen_lazim.firebase.FirebaseTokenManager

/** Hatalar Result.failure: bağlantı yoksa IOException, sunucu reddederse kodlu ApiException. */
class AuthRepository(
    private val api: AuthService = RetrofitClient.retrofit.create(AuthService::class.java)
) {

    suspend fun login(createUserDTO: CreateUserDTO, context: Context? = null): Result<Unit> = apiResult {
        saveTokens(api.login(createUserDTO).bodyOrThrow()?.data)

        // Initialize FCM token after successful login
        context?.let {
            try {
                FirebaseTokenManager.initializeFcmToken(it)
                Log.d("AuthRepository", "FCM token initialization started")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to initialize FCM token", e)
                // Don't fail login if FCM token fails
            }
        }
    }

    suspend fun register(createUserDTO: CreateUserDTO): Result<UserDTO> = apiResult {
        api.register(createUserDTO).bodyOrThrow()?.data ?: error("Register response has no user")
    }

    suspend fun refresh(refreshToken: String): Result<Unit> = apiResult {
        saveTokens(api.refresh(RefreshRequest(refreshToken)).bodyOrThrow()?.data)
    }

    private fun saveTokens(tokens: TokenResponse?) {
        check(tokens != null && tokens.accessToken.isNotBlank() && tokens.refreshToken.isNotBlank()) {
            "Token response is empty"
        }
        SharedPreferencesProvider.saveSession(tokens.accessToken, tokens.refreshToken)
    }

    suspend fun logout(): Result<Unit> {
        // Ağdan bağımsız ve anında: canlı mesajlar bu hesaptan kopsun
        WebSocketManager.disconnect()

        try {
            // Access token henüz silinmeden (uç nokta kimlik istiyor): bu cihaz artık bu
            // hesabın bildirimlerini almasın. Girişte initializeFcmToken'ın karşılığı.
            FirebaseTokenManager.unregisterFromServer()

            val refresh = SharedPreferencesProvider.getRefreshToken()
                ?: return Result.success(Unit).also { SharedPreferencesProvider.clearSession() }

            return try {
                val res = api.logout(RefreshRequest(refresh))
                // Başarılı ya da değil — local session'ı temizle
                SharedPreferencesProvider.clearSession()
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(res.toApiException())
            } catch (_: Exception) {
                SharedPreferencesProvider.clearSession()
                Result.success(Unit)
            }
        } finally {
            // Oturum silindikten sonra (zaman aşımında da): çıkış sürerken gelen bir rozet
            // yenilemesi bu hesabın sayılarını geri yazmış olabilir
            ChatUnreadStore.clear()
        }
    }
}
