package com.tpl.hemen_lazim.ui

import android.content.res.Resources
import androidx.annotation.StringRes
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
    private val scope: CoroutineScope,
    private val resources: Resources
) {
    fun show(message: String) {
        scope.launch {
            // Üst üste gelen mesajlar beklemesin; en sonuncusu görünür
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message)
        }
    }

    /** ViewModel'lerin tuttuğu strings.xml kimliği; metin telefonun dilinde çözülür. */
    fun show(@StringRes message: Int) = show(resources.getString(message))
}

val LocalAppSnackbar = staticCompositionLocalOf<AppSnackbar> {
    error("AppSnackbar sağlanmadı: ekranlar kök Scaffold'un içinde çizilmeli")
}
