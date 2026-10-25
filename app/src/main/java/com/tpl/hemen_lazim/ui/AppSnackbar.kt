package com.tpl.hemen_lazim.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Uygulama genelindeki tek mesaj kanalı (Toast yerine). Kök ekranın ömrüne bağlı kapsamda
 * gösterir: mesajı veren ekran kapansa da (ör. talep yayınlanıp İlanlarım'a geçilince) mesaj
 * yarıda kesilmez.
 */
class AppSnackbar(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String) {
        scope.launch {
            // Üst üste gelen mesajlar beklemesin; en sonuncusu görünür
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }
}

val LocalAppSnackbar = staticCompositionLocalOf<AppSnackbar> {
    error("AppSnackbar sağlanmadı: ekranlar kök Scaffold'un içinde çizilmeli")
}
