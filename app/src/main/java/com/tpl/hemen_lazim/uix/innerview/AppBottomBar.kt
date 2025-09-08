package com.tpl.hemen_lazim.uix.innerview


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.navigation.NavController
@Composable
private fun AppBottomBar(
    navController: NavController,
    currentRoute: String
) {
    val items = listOf(
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
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun ScreenScaffold(
    navController: NavController,
    currentRoute: String,
    topBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { topBar?.invoke() },
        bottomBar = { AppBottomBar(navController, currentRoute) }
    ) { padding -> content(padding) }
}
