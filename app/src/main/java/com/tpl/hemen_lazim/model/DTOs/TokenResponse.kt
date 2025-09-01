package com.tpl.hemen_lazim.model.DTOs

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
