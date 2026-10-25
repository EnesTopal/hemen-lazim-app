package com.tpl.hemen_lazim.uix.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.AuthUiState
import com.tpl.hemen_lazim.model.DTOs.CreateUserDTO
import com.tpl.hemen_lazim.network.repositories.AuthRepository
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
                s.username.isBlank() -> "Kullanıcı adı zorunlu"
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
                if (res.isSuccess) {
                    _ui.update { it.copy(isLoading = false) }
                    onLoginSuccessNavigate()
                } else {
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            toastMessage = res.exceptionOrNull()?.message ?: "Hata"
                        )
                    }
                }
            } else {
                val body =
                    CreateUserDTO(userName = s.username, userPassword = s.password, email = s.email)
                val res = repo.register(body)
                if (res.isSuccess) {
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            isLogin = true,
                            toastMessage = "Kayıt başarılı, şimdi giriş yapın"
                        )
                    }
                    onRegisterSwitchedToLogin()
                } else {
                    // Sunucu İngilizce konuşuyor; bilinen iki çakışma ilgili alanın altında Türkçe
                    // gösterilir, geri kalan mesajlar olduğu gibi
                    val msg = res.exceptionOrNull()?.message ?: "Hata"
                    val usernameTaken = msg.contains("Username already in use", ignoreCase = true)
                    val emailTaken = msg.contains("Email already in use", ignoreCase = true)
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            usernameError = if (usernameTaken) "Bu kullanıcı adı alınmış" else it.usernameError,
                            emailError = if (emailTaken) "Bu e-posta zaten kayıtlı" else it.emailError,
                            toastMessage = if (usernameTaken || emailTaken) null else msg
                        )
                    }
                }
            }
        }
    }
}
