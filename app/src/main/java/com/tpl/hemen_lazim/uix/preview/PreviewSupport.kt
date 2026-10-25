package com.tpl.hemen_lazim.uix.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tpl.hemen_lazim.navigation.AppBottomBar
import com.tpl.hemen_lazim.ui.theme.Hemen_lazimTheme

/** Tam ekran önizleme: telefon boyutunda, açık ve koyu temada. */
@Preview(name = "Açık tema", device = "spec:width=411dp,height=891dp")
@Preview(
    name = "Koyu tema",
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class ScreenPreviews

/** Ekranın bir parçası (ör. alt sayfa içeriği): içeriği kadar, açık ve koyu temada. */
@Preview(name = "Açık tema")
@Preview(name = "Koyu tema", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class ComponentPreviews

/**
 * Önizlenen ekran uygulamanın temasıyla; [tab] verilirse kök ekrandaki (AppNavHost) gibi alt
 * barla. Harita önizlemede çizilmiyor, HemenMapCard yerine sade bir çizim koyuyor.
 */
@Composable
internal fun PreviewScreen(tab: String? = null, content: @Composable () -> Unit) {
    Hemen_lazimTheme {
        if (tab == null) {
            content()
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                bottomBar = { AppBottomBar(currentRoute = tab, unreadTotal = PreviewData.UNREAD_TOTAL, onTabSelected = {}) }
            ) { padding ->
                Box(Modifier.padding(padding).consumeWindowInsets(padding)) { content() }
            }
        }
    }
}

/** Ekran parçası önizlemesi: tema ve yüzey rengi. */
@Composable
internal fun PreviewSurface(content: @Composable () -> Unit) {
    Hemen_lazimTheme { Surface(content = content) }
}
