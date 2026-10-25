package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.AuthUiState
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.theme.BrandOrange
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.AuthBlock
import com.tpl.hemen_lazim.uix.preview.PreviewData
import com.tpl.hemen_lazim.uix.preview.PreviewScreen
import com.tpl.hemen_lazim.uix.preview.ScreenPreviews
import com.tpl.hemen_lazim.uix.viewmodel.AuthViewModel

@Composable
fun Auth(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val snackbar = LocalAppSnackbar.current
    val vm: AuthViewModel = viewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.toastMessage) {
        ui.toastMessage?.let {
            snackbar.show(it)
            vm.clearToast()
        }
    }

    AuthContent(
        ui = ui,
        onModeChange = vm::setMode,
        onUsernameChange = vm::onUsernameChange,
        onPasswordChange = vm::onPasswordChange,
        onEmailChange = vm::onEmailChange,
        onSubmit = {
            vm.submit(
                appContext = context.applicationContext,
                onLoginSuccessNavigate = onLoggedIn,
                onRegisterSwitchedToLogin = {}
            )
        }
    )
}

@Composable
private fun AuthContent(
    ui: AuthUiState,
    onModeChange: (isLogin: Boolean) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // Klavye açıkken alanlar ve buton kaydırılarak görünür kalsın
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()
            Spacer(Modifier.height(Spacing.xxl))
            AuthBlock(
                ui = ui,
                onModeChange = onModeChange,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onEmailChange = onEmailChange,
                onSubmit = onSubmit
            )
        }
    }
}

/** Uygulama simgesi, adı ve tek cümlelik anlatım. */
@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(BrandOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Uyarlanabilir ikonun ön yüzü: simge tuvalin ortasında, launcher'daki oranla görünür
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(Spacing.l))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.auth_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@ScreenPreviews
@Composable
internal fun AuthPreview() {
    PreviewScreen {
        AuthContent(
            ui = PreviewData.auth,
            onModeChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onEmailChange = {},
            onSubmit = {}
        )
    }
}
