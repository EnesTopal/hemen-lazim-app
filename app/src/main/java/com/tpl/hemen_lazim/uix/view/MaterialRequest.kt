package com.tpl.hemen_lazim.uix.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tpl.hemen_lazim.uix.innerview.ScreenScaffold


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialRequest(navController: NavController) {
    ScreenScaffold (navController, currentRoute = "MaterialRequest",
        topBar = { CenterAlignedTopAppBar(title = { Text("Malzeme Talepleri") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Talepler", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            // TODO: Talep listesi
        }

        // Yeni talep ekleme (FAB’i alt sağa sabitlemek istersen ayrı Box kullan)
        Box(Modifier.fillMaxSize().padding(padding)) {
            FloatingActionButton(
                onClick = { /* TODO: yeni talep oluştur */ },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Yeni Talep") }
        }
    }
}