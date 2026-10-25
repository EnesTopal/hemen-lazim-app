package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.DTOs.ExtendRequestDTO
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestCreateDTO
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.apiResult
import com.tpl.hemen_lazim.network.bodyOrThrow
import com.tpl.hemen_lazim.network.services.AdminService
import com.tpl.hemen_lazim.network.services.RequestService

/** Hatalar Result.failure: bağlantı yoksa IOException, sunucu reddederse kodlu ApiException. */
class RequestRepository(
    private val api: RequestService = RetrofitClient.retrofit.create(RequestService::class.java),
    private val adminApi: AdminService = RetrofitClient.retrofit.create(AdminService::class.java)
) {

    suspend fun create(body: MaterialRequestCreateDTO): Result<Unit> = apiResult {
        api.create(body).bodyOrThrow()
        Unit
    }

    suspend fun listNearby(
        lat: Double,
        lng: Double,
        radius: Int? = null,
        category: Category? = null
    ): Result<List<MaterialRequestDTO>> = apiResult {
        api.nearby(lat, lng, radius, category).bodyOrThrow()?.data.orEmpty()
    }

    suspend fun listMine(): Result<List<MaterialRequestDTO>> = apiResult {
        api.mine().bodyOrThrow()?.data.orEmpty()
    }

    suspend fun cancel(id: String): Result<Unit> = apiResult {
        api.cancel(id).bodyOrThrow()
        Unit
    }

    suspend fun complete(id: String): Result<Unit> = apiResult {
        api.complete(id).bodyOrThrow()
        Unit
    }

    /** Açık talebin süresini uzatır; güncel talebi döner (yeni bitiş zamanıyla). */
    suspend fun extend(id: String, minutes: Int): Result<MaterialRequestDTO> = apiResult {
        api.extend(id, ExtendRequestDTO(minutes)).bodyOrThrow()?.data
            ?: error("Extend response has no request")
    }

    suspend fun adminGetAll(): Result<List<MaterialRequestDTO>> = apiResult {
        adminApi.getAllRequests().bodyOrThrow()?.data.orEmpty()
    }
}
