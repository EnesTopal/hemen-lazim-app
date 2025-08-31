package com.tpl.hemen_lazim.network.services

import com.tpl.hemen_lazim.model.HealthCheckResponse
import retrofit2.http.GET

interface HealthCheckService {
    @GET("actuator/health")
    suspend fun checkHealth(): HealthCheckResponse
}