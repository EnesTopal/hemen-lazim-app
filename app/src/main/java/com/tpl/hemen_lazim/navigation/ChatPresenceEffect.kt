package com.tpl.hemen_lazim.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.delay

/**
 * Uygulama öne geldiğinde soketi ayağa kaldırır ve okunmamış sayılarını tazeler. Oturum
 * açıkken tüm ekranlarda (sohbet ve talep formu dahil) çalışır.
 *
 * Tazeleme bilerek sunucudan yapılıyor: uygulama kapalıyken gelen mesajların rozeti
 * ancak böyle doğru görünür, canlı WebSocket sinyali tek başına yeterli değil.
 */
@Composable
fun ChatPresenceEffect() {
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
    // doğrulanır, böylece soket sessizce düşmüş olsa bile rozet doğru kalır.
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
