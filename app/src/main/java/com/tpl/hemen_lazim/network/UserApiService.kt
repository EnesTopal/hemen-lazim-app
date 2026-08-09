package com.tpl.hemen_lazim.network

import com.tpl.hemen_lazim.model.UserDto
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApiService {
    @GET("/api/users/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto
}
