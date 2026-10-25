package com.tpl.hemen_lazim.uix.view

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.navigation.Routes
import com.tpl.hemen_lazim.network.repositories.AuthRepository
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.theme.Spacing
import com.tpl.hemen_lazim.uix.innerview.Avatar
import com.tpl.hemen_lazim.uix.preview.PreviewData
import com.tpl.hemen_lazim.uix.preview.PreviewScreen
import com.tpl.hemen_lazim.uix.preview.ScreenPreviews
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun Profile(
    onOpenMyRequests: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalAppSnackbar.current
    val userName = remember { SharedPreferencesProvider.getPreferredUsername() ?: "Kullanıcı" }
    val version = remember { appVersion(context) }
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }

    ProfileContent(
        userName = userName,
        version = version,
        isLoggingOut = isLoggingOut,
        onOpenMyRequests = onOpenMyRequests,
        // İzin sistem ayarında: "Şimdi değil" diyen ya da reddeden buradan açabilir
        onOpenNotificationSettings = { context.startActivity(notificationSettingsIntent(context)) },
        onLogout = {
            isLoggingOut = true
            scope.launch {
                // Sunucu çağrıları (cihaz kaydı, refresh token iptali) en fazla birkaç
                // saniye beklenir; ağ yoksa da çıkış tamamlanır.
                withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { authRepo.logout() }
                SharedPreferencesProvider.clearSession() // zaman aşımında da token kalmasın
                snackbar.show("Çıkış yapıldı")
                onLoggedOut()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    userName: String,
    version: String?,
    isLoggingOut: Boolean,
    onOpenMyRequests: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.tab_profile)) }) }) {
            padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar(userName, size = 72.dp)
                Spacer(Modifier.height(Spacing.m))
                Text(userName, style = MaterialTheme.typography.headlineSmall)
            }

            HorizontalDivider()
            ProfileRow(
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                title = stringResource(R.string.tab_my_requests),
                subtitle = stringResource(R.string.profile_my_requests_hint),
                onClick = onOpenMyRequests
            )
            ProfileRow(
                icon = Icons.Outlined.NotificationsActive,
                title = stringResource(R.string.profile_notifications),
                subtitle = stringResource(R.string.profile_notifications_hint),
                onClick = onOpenNotificationSettings
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(enabled = !isLoggingOut) { confirmLogout = true },
                headlineContent = {
                    Text(
                        stringResource(if (isLoggingOut) R.string.profile_logging_out else R.string.profile_logout),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )

            Spacer(Modifier.height(Spacing.xl))
            version?.let {
                Text(
                    stringResource(R.string.profile_version, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(R.string.profile_logout_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.profile_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) }
    )
}

private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

private fun appVersion(context: Context): String? =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()

private const val LOGOUT_TIMEOUT_MS = 5000L

@ScreenPreviews
@Composable
internal fun ProfilePreview() {
    PreviewScreen(tab = Routes.PROFILE) {
        ProfileContent(
            userName = PreviewData.USER_NAME,
            version = "1.0",
            isLoggingOut = false,
            onOpenMyRequests = {},
            onOpenNotificationSettings = {},
            onLogout = {}
        )
    }
}
