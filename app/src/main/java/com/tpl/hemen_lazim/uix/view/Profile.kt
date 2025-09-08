package com.tpl.hemen_lazim.uix.view

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tpl.hemen_lazim.uix.innerview.ScreenScaffold
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Profile(navController: NavController) {
    val context = LocalContext.current
    val userName = remember { SharedPreferencesProvider.getPreferredUsername() ?: "Kullanıcı" }

    ScreenScaffold(navController, currentRoute = "Profile",
        topBar = { CenterAlignedTopAppBar(title = { Text("Profil") }) }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text(userName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            ElevatedCard(Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("MaterialRequest") { launchSingleTop = true }
                }) {
                ListItem(
                    headlineContent = { Text("İsteklerim") },
                    supportingContent = { Text("Oluşturduğun / takip ettiğin talepler") },
                    leadingContent = { Icon(Icons.Default.Assignment, contentDescription = null) }
                )
            }
            Spacer(Modifier.height(8.dp))

            ElevatedCard(Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("Chats") { launchSingleTop = true }
                }) {
                ListItem(
                    headlineContent = { Text("Sohbetlerim") },
                    supportingContent = { Text("Görüşmeler ve bildirimler") },
                    leadingContent = { Icon(Icons.Default.Chat, contentDescription = null) }
                )
            }
            Spacer(Modifier.height(8.dp))

            ElevatedCard(Modifier
                .fillMaxWidth()
                .clickable {
                    SharedPreferencesProvider.clearSession() // erişim/yenileme tokenlarını sil
                    Toast.makeText(context, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
                    navController.navigate("Auth") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }) {
                ListItem(
                    headlineContent = { Text("Çıkış Yap") },
                    supportingContent = { Text("Hesabından güvenle çık") },
                    leadingContent = { Icon(Icons.Default.Logout, contentDescription = null) }
                )
            }
        }
    }
}
