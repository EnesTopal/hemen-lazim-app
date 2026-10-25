package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.network.repositories.AuthRepository
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Profile(
    onOpenMyRequests: () -> Unit,
    onOpenChats: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val snackbar = LocalAppSnackbar.current
    val userName = remember { SharedPreferencesProvider.getPreferredUsername() ?: "Kullanıcı" }
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.tab_profile)) }) }) {
            padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text(userName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            ElevatedCard(Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenMyRequests)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.tab_my_requests)) },
                    supportingContent = { Text("Açtığın talepler") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null) }
                )
            }
            Spacer(Modifier.height(8.dp))

            ElevatedCard(Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenChats)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.tab_chats)) },
                    supportingContent = { Text("Görüşmeler ve bildirimler") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) }
                )
            }
            Spacer(Modifier.height(8.dp))

            ElevatedCard(Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoggingOut) {
                    isLoggingOut = true
                    scope.launch {
                        // Sunucu çağrıları (cihaz kaydı, refresh token iptali) en fazla birkaç
                        // saniye beklenir; ağ yoksa da çıkış tamamlanır.
                        withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { authRepo.logout() }
                        SharedPreferencesProvider.clearSession() // zaman aşımında da token kalmasın
                        snackbar.show("Çıkış yapıldı")
                        onLoggedOut()
                    }
                }) {
                ListItem(
                    headlineContent = { Text(if (isLoggingOut) "Çıkış yapılıyor..." else "Çıkış Yap") },
                    supportingContent = { Text("Hesabından güvenle çık") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) }
                )
            }
        }
    }
}

private const val LOGOUT_TIMEOUT_MS = 5000L
