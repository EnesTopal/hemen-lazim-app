package com.tpl.hemen_lazim.model

import androidx.annotation.StringRes

data class AuthUiState(
    val isLogin: Boolean = true,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    // Mesajlar strings.xml kimliği: metin telefonun dilinde ekranda çözülür
    @StringRes val toastMessage: Int? = null,
    @StringRes val usernameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val formError: Int? = null
) {
    val canSubmit: Boolean
        get() = !isLoading &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                usernameError == null &&
                (if (isLogin) true else email.isNotBlank()) &&
                (if (isLogin) true else emailError == null) &&
                (if (isLogin) true else passwordError == null)
}