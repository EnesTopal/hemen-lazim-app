package com.tpl.hemen_lazim.model.DTOs

/** POST /api/requests/{id}/extend gövdesi: eklenecek süre (15–1440 dk). */
data class ExtendRequestDTO(
    val minutes: Int
)
