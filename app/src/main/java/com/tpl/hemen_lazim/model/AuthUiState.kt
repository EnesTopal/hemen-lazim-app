package com.tpl.hemen_lazim.model

data class AuthUiState(
    val isLogin: Boolean = true,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null
) {
    val canSubmit: Boolean
        get() = !isLoading &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                (if (isLogin) true else email.isNotBlank()) &&
                (if (isLogin) true else emailError == null) &&
                (if (isLogin) true else passwordError == null)
}