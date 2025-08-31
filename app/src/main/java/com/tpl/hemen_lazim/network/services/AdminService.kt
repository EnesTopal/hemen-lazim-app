package com.tpl.hemen_lazim.network.services

import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import retrofit2.http.GET

interface AdminService {
    @GET("admin/requests")
    suspend fun getAllRequests(): ApiResponse<List<MaterialRequestDTO>>
}