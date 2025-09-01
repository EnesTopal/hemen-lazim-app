package com.tpl.hemen_lazim.uix.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.AuthUiState
import com.tpl.hemen_lazim.model.DTOs.CreateUserDTO
import com.tpl.hemen_lazim.network.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun toggleMode() = _ui.update { it.copy(isLogin = !it.isLogin, toastMessage = null) }
    fun onUsernameChange(v: String) = _ui.update { it.copy(username = v, toastMessage = null) }
    fun onEmailChange(v: String) = _ui.update { it.copy(email = v, toastMessage = null) }
    fun onPasswordChange(v: String) = _ui.update { it.copy(password = v, toastMessage = null) }
    fun clearToast() = _ui.update { it.copy(toastMessage = null) }

    fun submit(onLoginSuccessNavigate: () -> Unit, onRegisterSwitchedToLogin: () -> Unit) {
        val state = _ui.value

        if (state.username.isBlank() || state.password.isBlank()) {
            _ui.update { it.copy(toastMessage = "Kullanıcı adı ve şifre zorunlu") }
            return
        }
        if (!state.isLogin && state.email.isBlank()) {
            _ui.update { it.copy(toastMessage = "E-posta zorunlu") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, toastMessage = null) }

            if (state.isLogin) {
                val res = repo.login(CreateUserDTO(state.username, state.password))
                if (res.isSuccess) {
                    _ui.update { it.copy(isLoading = false, toastMessage = "Login başarılı") }
                    onLoginSuccessNavigate()
                } else {
                    _ui.update { it.copy(isLoading = false, toastMessage = res.exceptionOrNull()?.message ?: "Hata") }
                }
            } else {
                val body = CreateUserDTO(
                    userName = state.username,
                    userPassword = state.password,
                    email = state.email
                )
                val res = repo.register(body)
                if (res.isSuccess) {
                    toggleMode()
                    _ui.update { it.copy(isLoading = false, toastMessage = "Kayıt başarılı, şimdi giriş yapın") }
                    onRegisterSwitchedToLogin()
                } else {
                    _ui.update { it.copy(isLoading = false, toastMessage = res.exceptionOrNull()?.message ?: "Hata") }
                }
            }
        }
    }
}
