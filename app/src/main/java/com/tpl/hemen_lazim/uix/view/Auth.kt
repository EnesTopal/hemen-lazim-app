package com.tpl.hemen_lazim.uix.view

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.uix.innerview.AuthBlock
import com.tpl.hemen_lazim.uix.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Auth(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val snackbar = LocalAppSnackbar.current
    val vm: AuthViewModel = viewModel()
    val ui by vm.ui.collectAsState()

    LaunchedEffect(ui.toastMessage) {
        ui.toastMessage?.let {
            snackbar.show(it)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (ui.isLogin) "Login" else "Register") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthBlock(
                isLogin = ui.isLogin,
                email = ui.email,
                emailError = ui.emailError,
                username = ui.username,
                password = ui.password,
                passwordError = ui.passwordError,
                formError = ui.formError,
                canSubmit = ui.canSubmit,
                isLoading = ui.isLoading,
                onToggleMode = vm::toggleMode,
                onUsernameChange = vm::onUsernameChange,
                onPasswordChange = vm::onPasswordChange,
                onEmailChange = vm::onEmailChange,
                onSubmit = {
                    vm.submit(
                        appContext = context.applicationContext,
                        onLoginSuccessNavigate = onLoggedIn,
                        onRegisterSwitchedToLogin = {  }
                    )
                }
            )
        }
    }
}
