package com.tpl.hemen_lazim.uix.innerview


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.delay

@Composable
private fun AppBottomBar(navController: NavController, currentRoute: String) {
    val unreadByRoom by ChatUnreadStore.unreadByRoom.collectAsState()
    val hasUnread = unreadByRoom.values.any { it > 0 }

    val items =
            listOf(
                    Triple("Chats", "Sohbetler", Icons.AutoMirrored.Filled.Chat),
                    Triple("MaterialRequest", "Talepler", Icons.AutoMirrored.Filled.Assignment),
                    Triple("Profile", "Profil", Icons.Default.Menu)
            )
    NavigationBar {
        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    },
                    icon = {
                        if (route == "Chats" && hasUnread) {
                            BadgedBox(badge = { Badge(containerColor = Color.Red) }) {
                                Icon(icon, contentDescription = label)
                            }
                        } else {
                            Icon(icon, contentDescription = label)
                        }
                    },
                    label = { Text(label) }
            )
        }
    }
}

/**
 * Uygulama öne geldiğinde soketi ayağa kaldırır ve okunmamış sayılarını tazeler.
 *
 * Tazeleme bilerek sunucudan yapılıyor: uygulama kapalıyken gelen mesajların rozeti
 * ancak böyle doğru görünür, canlı WebSocket sinyali tek başına yeterli değil.
 */
@Composable
private fun ChatPresenceEffect() {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val wake = {
            if (!SharedPreferencesProvider.getAccessToken().isNullOrBlank()) {
                WebSocketManager.connect()
                ChatUnreadStore.refresh()
            }
        }
        // Observer yalnızca sonraki olayları alır; ekran zaten öndeyse ilk tetiklemeyi
        // biz yapmalıyız.
        wake()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) wake()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Rozet yalnızca WebSocket'e güvenmesin: ekran öndeyken seyrek de olsa sunucudan
    // doğrulanır, böylece soket sessizce düşmüş olsa bile kırmızı nokta doğru kalır.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(UNREAD_REFRESH_INTERVAL_MS)
                ChatUnreadStore.refresh()
            }
        }
    }
}

private const val UNREAD_REFRESH_INTERVAL_MS = 15000L

@Composable
fun ScreenScaffold(
        navController: NavController,
        currentRoute: String,
        topBar: (@Composable () -> Unit)? = null,
        content: @Composable (PaddingValues) -> Unit
) {
    ChatPresenceEffect()
    Scaffold(
            topBar = { topBar?.invoke() },
            bottomBar = { AppBottomBar(navController, currentRoute) }
    ) { padding -> content(padding) }
}
