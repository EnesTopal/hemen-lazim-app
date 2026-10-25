package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.DTOs.SupplyOfferNotificationDTO
import com.tpl.hemen_lazim.network.ApiCodes
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.apiResult
import com.tpl.hemen_lazim.network.bodyOrThrow
import com.tpl.hemen_lazim.network.services.NotificationService

class NotificationRepository(
    private val api: NotificationService = RetrofitClient.retrofit.create(NotificationService::class.java)
) {

    /**
     * true: talep sahibine şimdi haber verildi; false: bu talep için zaten haber vermiştin
     * (sunucu ikinci bir bildirim göndermez). Hatalar kodlu ApiException ya da IOException.
     */
    suspend fun sendSupplyOfferNotification(requestId: String, requesterId: String): Result<Boolean> = apiResult {
        val res = api.sendSupplyOfferNotification(SupplyOfferNotificationDTO(requestId, requesterId)).bodyOrThrow()
        res?.code != ApiCodes.REQUESTER_ALREADY_NOTIFIED
    }
}
