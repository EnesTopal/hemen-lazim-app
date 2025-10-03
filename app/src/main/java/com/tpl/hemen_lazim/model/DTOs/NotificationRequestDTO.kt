package com.tpl.hemen_lazim.model.DTOs

data class NotificationRequestDTO(
    val recipientUserId: String,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null
)

data class SupplyOfferNotificationDTO(
    val requestId: String,
    val requesterId: String
)

data class DeviceRegisterRequestDTO(
    val fcmToken: String,
    val deviceType: String? = "ANDROID",
    val deviceName: String? = null
)

