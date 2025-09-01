package com.tpl.hemen_lazim.uix.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tpl.hemen_lazim.uix.innerview.AuthBlock
import com.tpl.hemen_lazim.uix.viewmodel.AuthViewModel
import com.tpl.hemen_lazim.utils.DoubleBackToExit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Auth(
    navController: NavController,
    vm: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    DoubleBackToExit()

    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(ui.toastMessage) {
        ui.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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
                username = ui.username,
                password = ui.password,
                isLoading = ui.isLoading,
                onToggleMode = vm::toggleMode,
                onUsernameChange = vm::onUsernameChange,
                onPasswordChange = vm::onPasswordChange,
                onEmailChange = vm::onEmailChange,
                onSubmit = {
                    vm.submit(
                        onLoginSuccessNavigate = {
                            navController.navigate("MaterialRequest") {
                                popUpTo("Auth") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onRegisterSwitchedToLogin = {
                        }
                    )
                }
            )
        }
    }
}