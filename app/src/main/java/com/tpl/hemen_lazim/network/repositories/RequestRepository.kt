package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestCreateDTO
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.AdminService
import com.tpl.hemen_lazim.network.services.RequestApiService

class RequestRepository(
    private val api: RequestApiService = RetrofitClient.retrofit.create(RequestApiService::class.java),
    private val adminApi: AdminService = RetrofitClient.retrofit.create(AdminService::class.java)
) {
    suspend fun createRequest(body: MaterialRequestCreateDTO): ApiResponse<MaterialRequestDTO> =
        api.create(body)

    suspend fun listNearby(
        lat: Double,
        lng: Double,
        radius: Int? = null,
        category: Category? = null
    ): ApiResponse<List<MaterialRequestDTO>> = api.nearby(lat, lng, radius, category)

    suspend fun listMine(): ApiResponse<List<MaterialRequestDTO>> = api.mine()

    suspend fun cancel(id: String): ApiResponse<String> = api.cancel(id)

    suspend fun complete(id: String): ApiResponse<String> = api.complete(id)

    suspend fun adminGetAll(): ApiResponse<List<MaterialRequestDTO>> = adminApi.getAllRequests()
}