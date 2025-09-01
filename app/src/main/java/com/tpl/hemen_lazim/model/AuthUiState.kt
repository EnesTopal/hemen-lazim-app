package com.tpl.hemen_lazim.model

data class AuthUiState(
    val isLogin: Boolean = true,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)
