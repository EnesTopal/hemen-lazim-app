package com.tpl.hemen_lazim.model.DTOs

data class ApiResponse<T>(
    val message: String? = null,
    val data: T? = null,
    // Ayırt edilmesi gereken başarılı sonucun sabit kodu (ör. REQUESTER_ALREADY_NOTIFIED); çoğu yanıtta yok
    val code: String? = null
)