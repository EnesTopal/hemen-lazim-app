package com.tpl.hemen_lazim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tpl.hemen_lazim.navigation.AppNavHost
import com.tpl.hemen_lazim.session.SessionManager
import com.tpl.hemen_lazim.ui.theme.Hemen_lazimTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted: Boolean ->
                if (isGranted) {
                    Log.d("MainActivity", "Notification permission granted")
                } else {
                    Log.w("MainActivity", "Notification permission denied")
                    Toast.makeText(this, "Bildirimler için izin gerekli", Toast.LENGTH_LONG).show()
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        // super'dan önce: sistem açılış ekranı uygulama temasına geçişi yönetsin
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Yalnızca ilk açılışta: ekran dönünce aynı bildirim sohbeti yeniden açmasın
        if (savedInstanceState == null) viewModel.onIntent(intent)

        // Request notification permission for Android 13+
        requestNotificationPermission()

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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation if needed
                    Toast.makeText(this, "Bildirimleri almak için izin gerekli", Toast.LENGTH_LONG)
                            .show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("MainActivity", "Notification permission not needed for API < 33")
        }
    }
}
