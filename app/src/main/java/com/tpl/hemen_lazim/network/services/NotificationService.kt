package com.tpl.hemen_lazim.network.services

import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.DTOs.DeviceRegisterRequestDTO
import com.tpl.hemen_lazim.model.DTOs.SupplyOfferNotificationDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NotificationService {
    
    @POST("api/notifications/fcm-token")
    suspend fun registerDevice(@Body body: DeviceRegisterRequestDTO): Response<ApiResponse<String>>
    
    @POST("api/notifications/supply-offer")
    suspend fun sendSupplyOfferNotification(@Body body: SupplyOfferNotificationDTO): Response<ApiResponse<String>>
}

