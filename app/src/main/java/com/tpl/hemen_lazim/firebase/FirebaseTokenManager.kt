package com.tpl.hemen_lazim.firebase

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.NotificationService
import com.tpl.hemen_lazim.model.DTOs.DeviceRegisterRequestDTO
import kotlinx.coroutines.tasks.await

object FirebaseTokenManager {
    private const val TAG = "FirebaseTokenManager"

    /**
     * Initialize FCM token - Call this after user login
     */
    suspend fun initializeFcmToken(context: Context): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token obtained: $token")
            
            // Initialize SharedPreferences
            SharedPreferencesProvider.init(context)
            
            // Save locally
            SharedPreferencesProvider.saveFcmToken(token)
            
            // Get device info
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            
            // Send to server
            sendTokenToServer(token, deviceName)
            
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }

    /**
     * Send FCM token to backend server with device info
     */
    private suspend fun sendTokenToServer(token: String, deviceName: String): Boolean {
        return try {
            val service = RetrofitClient.retrofit.create(NotificationService::class.java)
            val request = DeviceRegisterRequestDTO(
                fcmToken = token,
                deviceType = "ANDROID",
                deviceName = deviceName
            )
            val response = service.registerDevice(request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Device registered successfully: $deviceName")
                true
            } else {
                Log.e(TAG, "Failed to register device: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device", e)
            false
        }
    }

    /**
     * Request FCM token and send to server
     */
    suspend fun refreshAndSendToken(context: Context) {
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
            initializeFcmToken(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh FCM token", e)
        }
    }
}

