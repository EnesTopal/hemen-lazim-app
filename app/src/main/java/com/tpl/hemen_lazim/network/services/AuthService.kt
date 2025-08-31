package com.tpl.hemen_lazim.network.services

import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.DTOs.CreateUserDTO
import com.tpl.hemen_lazim.model.DTOs.UserDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body loginRequest: CreateUserDTO): Response<ApiResponse<String>>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: CreateUserDTO): Response<ApiResponse<UserDTO>>
}