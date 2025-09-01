package com.tpl.hemen_lazim.model.DTOs

data class CreateUserDTO(
    val userName: String,
    val userPassword: String,
    val email: String? = null
)
