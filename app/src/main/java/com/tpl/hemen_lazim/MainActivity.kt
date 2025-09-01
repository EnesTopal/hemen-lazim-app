package com.tpl.hemen_lazim

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.tpl.hemen_lazim.network.repositories.HealthCheckRepository
import com.tpl.hemen_lazim.ui.theme.Hemen_lazimTheme
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = HealthCheckRepository()
        enableEdgeToEdge()
        setContent {
            Hemen_lazimTheme {
                var context = LocalContext.current
                var healthStatus = remember { mutableStateOf<String>("") }
                lifecycleScope.launch {
                    healthStatus.value = repository.checkServerHealth()
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "Server Status ${healthStatus.value} ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                SharedPreferencesProvider.init(this)
                PageOrientation()

            }
        }
    }
}

