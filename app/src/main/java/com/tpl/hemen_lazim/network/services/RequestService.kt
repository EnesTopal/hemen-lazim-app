package com.tpl.hemen_lazim.network.services

import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestCreateDTO
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import retrofit2.Response
import retrofit2.http.*

interface RequestService {

    @POST("requests")
    suspend fun create(@Body body: MaterialRequestCreateDTO)
            : Response<ApiResponse<MaterialRequestDTO>>

    @GET("requests/nearby")
    suspend fun nearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusMeters") radiusMeters: Int? = null,
        @Query("category") category: Category? = null
    ): Response<ApiResponse<List<MaterialRequestDTO>>>

    @GET("requests/mine")
    suspend fun mine(): Response<ApiResponse<List<MaterialRequestDTO>>>

    @POST("requests/{id}/cancel")
    suspend fun cancel(@Path("id") id: String): Response<ApiResponse<String>>

    @POST("requests/{id}/complete")
    suspend fun complete(@Path("id") id: String): Response<ApiResponse<String>>
}