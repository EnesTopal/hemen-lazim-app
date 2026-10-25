package com.tpl.hemen_lazim.uix.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.AuthUiState
import com.tpl.hemen_lazim.model.DTOs.CreateUserDTO
import com.tpl.hemen_lazim.network.ApiCodes
import com.tpl.hemen_lazim.network.ApiException
import com.tpl.hemen_lazim.network.repositories.AuthRepository
import com.tpl.hemen_lazim.ui.messageRes
import com.tpl.hemen_lazim.utils.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun toggleMode() = _ui.update {
        it.copy(
            isLogin = !it.isLogin,
            toastMessage = null,
            formError = null,
            usernameError = null,
            emailError = null,
            passwordError = null
        )
    }

    /** Segmentli düğmeden: zaten seçili moda basmak formu sıfırlamasın. */
    fun setMode(isLogin: Boolean) {
        if (_ui.value.isLogin != isLogin) toggleMode()
    }

    fun onUsernameChange(v: String) = _ui.update { it.copy(username = v, usernameError = null, toastMessage = null) }
    fun onEmailChange(v: String) = _ui.update { it.copy(email = v, emailError = null, toastMessage = null) }
    fun onPasswordChange(v: String) = _ui.update {
        val err = if (it.isLogin) null else AuthValidator.validatePassword(v)
        it.copy(password = v, passwordError = err, toastMessage = null, formError = null)
    }

    fun clearToast() = _ui.update { it.copy(toastMessage = null) }

    /**
     * [appContext] yalnızca girişten sonra FCM kaydı için; ViewModel Context tutmuyor (ekran dönünce
     * eski Activity'yi sızdırırdı), uygulama context'i çağrıda veriliyor.
     */
    fun submit(
        appContext: Context,
        onLoginSuccessNavigate: () -> Unit,
        onRegisterSwitchedToLogin: () -> Unit
    ) {
        val s = _ui.value

        val emailErr = if (s.isLogin) null else AuthValidator.validateEmail(s.email)
        val passErr = if (s.isLogin) null else AuthValidator.validatePassword(s.password)

        if ((!s.isLogin && emailErr != null) || (!s.isLogin && passErr != null) || s.username.isBlank()) {
            val firstMsg = when {
                s.username.isBlank() -> R.string.auth_error_username_required
                !s.isLogin && emailErr != null -> emailErr
                !s.isLogin && passErr != null -> passErr
                else -> null
            }
            _ui.update {
                it.copy(
                    emailError = emailErr,
                    passwordError = passErr,
                    formError = firstMsg
                )
            }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, toastMessage = null, formError = null) }

            if (s.isLogin) {
                val res = repo.login(CreateUserDTO(s.username, s.password), appContext)
                res.onSuccess {
                    _ui.update { it.copy(isLoading = false) }
                    onLoginSuccessNavigate()
                }.onFailure { e ->
                    _ui.update { it.copy(isLoading = false, toastMessage = e.messageRes()) }
                }
            } else {
                val body =
                    CreateUserDTO(userName = s.username, userPassword = s.password, email = s.email)
                val res = repo.register(body)
                res.onSuccess {
                    _ui.update {
                        it.copy(isLoading = false, isLogin = true, toastMessage = R.string.auth_registered)
                    }
                    onRegisterSwitchedToLogin()
                }.onFailure { e ->
                    // Bir alana ait sunucu hatası o alanın altında, diğerleri mesaj olarak
                    val message = e.messageRes()
                    _ui.update {
                        when ((e as? ApiException)?.code) {
                            ApiCodes.USERNAME_TAKEN, ApiCodes.USERNAME_INVALID ->
                                it.copy(isLoading = false, usernameError = message)
                            ApiCodes.EMAIL_TAKEN, ApiCodes.EMAIL_INVALID ->
                                it.copy(isLoading = false, emailError = message)
                            ApiCodes.PASSWORD_INVALID, ApiCodes.PASSWORD_TOO_LONG ->
                                it.copy(isLoading = false, passwordError = message)
                            else -> it.copy(isLoading = false, toastMessage = message)
                        }
                    }
                }
            }
        }
    }
}
