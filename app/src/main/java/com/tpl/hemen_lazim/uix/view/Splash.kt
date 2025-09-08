package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tpl.hemen_lazim.network.repositories.AuthRepository
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

@Composable
fun Splash(navController: NavController, repo: AuthRepository) {
    LaunchedEffect(Unit) {
        val ok = ensureSessionValid(repo)
        if (ok) {
            navController.navigate("MaterialRequest") {
                popUpTo("Auth") { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate("Auth") {
                popUpTo("Auth") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Açılıyor...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private suspend fun ensureSessionValid(repo: AuthRepository): Boolean {
    val access = SharedPreferencesProvider.getAccessToken()
    val refresh = SharedPreferencesProvider.getRefreshToken()
    val now = System.currentTimeMillis() / 1000
    val exp = SharedPreferencesProvider.accessTokenExpSeconds()

    if (access.isNullOrBlank()) {
        return tryRefresh(repo, refresh)
    }

    val isExpired = exp == null || now >= exp
    val aboutToExpire = SharedPreferencesProvider.isAccessTokenAboutToExpire(60)

    if (isExpired || aboutToExpire) {
        return tryRefresh(repo, refresh)
    }

    return true
}

private suspend fun tryRefresh(repo: AuthRepository, refresh: String?): Boolean {
    if (refresh.isNullOrBlank()) return false
    val res = repo.refresh(refresh)
    return res.isSuccess
}
