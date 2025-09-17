package com.tpl.hemen_lazim.model.DTOs

import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.Units

data class MaterialRequestCreateDTO(
    val title: String,
    val description: String? = null,
    val category: Category,
    val quantity: Int? = null,
    val units: Units,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int? = null,
    val expiresAt: String? = null
)
