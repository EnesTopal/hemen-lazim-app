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

/** Tam ekran önizleme: telefon boyutunda; Türkçe açık ve koyu temada, bir de İngilizce. */
@Preview(name = "Türkçe, açık tema", locale = "tr", device = "spec:width=411dp,height=891dp")
@Preview(
    name = "Türkçe, koyu tema",
    locale = "tr",
    device = "spec:width=411dp,height=891dp",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(name = "İngilizce", locale = "en", device = "spec:width=411dp,height=891dp")
annotation class ScreenPreviews

/** Ekranın bir parçası (ör. alt sayfa içeriği): içeriği kadar; Türkçe açık ve koyu temada, bir de İngilizce. */
@Preview(name = "Türkçe, açık tema", locale = "tr")
@Preview(name = "Türkçe, koyu tema", locale = "tr", uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "İngilizce", locale = "en")
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
