package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.DTOs.CreateUserDTO
import com.tpl.hemen_lazim.model.DTOs.UserDTO
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.AuthService

class AuthRepository(
    private val api: AuthService = RetrofitClient.retrofit.create(AuthService::class.java)
) {
    suspend fun login(username: String, password: String): ApiResponse<String> {
        return api.login(CreateUserDTO(username = username, userpassword = password))
    }

    suspend fun register(username: String, password: String): ApiResponse<UserDTO> {
        return api.register(CreateUserDTO(username = username, userpassword = password))
    }
}