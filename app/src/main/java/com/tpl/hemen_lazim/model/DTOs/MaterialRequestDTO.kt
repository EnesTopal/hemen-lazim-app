package com.tpl.hemen_lazim.model.DTOs

import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.RequestStatus
import com.tpl.hemen_lazim.model.enums.Unit

data class MaterialRequestDTO(
    val id: String,
    val requesterId: String?,
    val requesterName: String?,
    val title: String,
    val description: String?,
    val category: Category,
    val quantity: Int?,
    val unit: Unit?,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int?,
    val status: RequestStatus,
    val expiresAt: String?,
    val createdAt: String
)
