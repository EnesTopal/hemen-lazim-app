package com.tpl.hemen_lazim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tpl.hemen_lazim.navigation.AppNavHost
import com.tpl.hemen_lazim.session.SessionManager
import com.tpl.hemen_lazim.ui.theme.Hemen_lazimTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // super'dan önce: sistem açılış ekranı uygulama temasına geçişi yönetsin
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Yalnızca ilk açılışta: ekran dönünce aynı bildirim sohbeti yeniden açmasın
        if (savedInstanceState == null) viewModel.onIntent(intent)

        // Bildirim izni burada değil, ilk talep yayınlanınca soruluyor (CreateRequest)

        // Yerel karar, ağ beklemiyor: açılış ekranı anında kalkar
        val startsLoggedIn = SessionManager.hasSession()

        enableEdgeToEdge()
        setContent {
            Hemen_lazimTheme {
                val pendingChat by viewModel.pendingChat.collectAsStateWithLifecycle()
                AppNavHost(
                        startsLoggedIn = startsLoggedIn,
                        pendingChat = pendingChat,
                        onChatOpened = viewModel::onChatOpened
                )
            }
        }
    }

    // Uygulama açıkken bildirime dokunulunca (FLAG_ACTIVITY_SINGLE_TOP) buraya gelir;
    // eskiden yalnızca onCreate'teki intent okunuyordu ve sohbet açılmıyordu.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.onIntent(intent)
    }
}
