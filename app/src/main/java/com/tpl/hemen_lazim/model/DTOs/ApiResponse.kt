package com.tpl.hemen_lazim.model.DTOs

data class ApiResponse<T>(
    val message: String? = null,
    val data: T? = null
)