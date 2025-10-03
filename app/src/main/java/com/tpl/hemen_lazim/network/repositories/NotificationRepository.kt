package com.tpl.hemen_lazim.network.repositories

import android.util.Log
import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.DTOs.SupplyOfferNotificationDTO
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.NotificationService
import retrofit2.HttpException
import java.io.IOException

class NotificationRepository(
    private val api: NotificationService = RetrofitClient.retrofit.create(NotificationService::class.java)
) {
    private val TAG = "NotificationRepository"

    suspend fun sendSupplyOfferNotification(requestId: String, requesterId: String): Result<String> {
        return try {
            val dto = SupplyOfferNotificationDTO(requestId, requesterId)
            val res = api.sendSupplyOfferNotification(dto)
            
            if (res.isSuccessful) {
                val message = res.body()?.message ?: "Notification sent"
                Log.d(TAG, "Notification sent successfully")
                Result.success(message)
            } else {
                val errorMsg = extractApiMessage(res.errorBody()?.string())
                Log.e(TAG, "Failed to send notification: $errorMsg")
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending notification", e)
            Result.failure(IOException("Bağlantı hatası: ${e.message}", e))
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error sending notification", e)
            Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending notification", e)
            Result.failure(e)
        }
    }

    private fun extractApiMessage(errorBody: String?): String {
        return try {
            errorBody?.let {
                val json = org.json.JSONObject(it)
                json.optString("message", "Bilinmeyen hata")
            } ?: "Bilinmeyen hata"
        } catch (e: Exception) {
            "Bilinmeyen hata"
        }
    }
}

