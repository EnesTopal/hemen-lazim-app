package com.tpl.hemen_lazim

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.session.SessionEvents
import com.tpl.hemen_lazim.ui.AppSnackbar
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.chat.ChatScreen
import com.tpl.hemen_lazim.uix.view.Auth
import com.tpl.hemen_lazim.uix.view.Chats
import com.tpl.hemen_lazim.uix.view.MaterialRequest
import com.tpl.hemen_lazim.uix.view.Profile
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

@Composable
fun PageOrientation(
        startsLoggedIn: Boolean,
        pendingChat: ChatDeepLink?,
        onChatOpened: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appSnackbar = remember { AppSnackbar(snackbarHostState, scope) }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val currentOnChatOpened by rememberUpdatedState(onChatOpened)

    // Oturum çıkış yapılmadan düştü (refresh token reddedildi): girişe dön, nedenini söyle
    LaunchedEffect(navController) {
        SessionEvents.expired.collect {
            WebSocketManager.disconnect()
            ChatUnreadStore.clear()
            if (navController.currentDestination?.route != "Auth") {
                navController.navigate("Auth") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
                appSnackbar.show("Oturumun sona erdi, lütfen tekrar giriş yap")
            }
        }
    }

    // Bildirimden gelen sohbet: oturum açıksa hemen, değilse girişten sonra açılır
    LaunchedEffect(pendingChat, currentRoute) {
        val link = pendingChat ?: return@LaunchedEffect
        if (currentRoute == null || currentRoute == "Auth") return@LaunchedEffect
        val me = SharedPreferencesProvider.getUserId() ?: return@LaunchedEffect
        navController.navigate("Chat/${link.roomId}/$me/${link.otherUserId}") {
            launchSingleTop = true
        }
        currentOnChatOpened()
    }

    CompositionLocalProvider(LocalAppSnackbar provides appSnackbar) {
        Scaffold(
                // Ekranlar kendi çubuklarını ve kenar boşluklarını yönetiyor
                contentWindowInsets = WindowInsets(0),
                snackbarHost = {
                    SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding().imePadding())
                }
        ) { padding ->
            NavHost(
                    navController = navController,
                    startDestination = if (startsLoggedIn) "MaterialRequest" else "Auth",
                    modifier = Modifier.padding(padding)
            ) {
                composable("Auth") { Auth(navController = navController) }
                composable("Profile") { Profile(navController = navController) }
                composable("MaterialRequest") { MaterialRequest(navController = navController) }
                composable("Chats") { Chats(navController = navController) }

                composable(
                        "Chat/{roomId}/{userId}/{recipientId}",
                        arguments =
                                listOf(
                                        navArgument("roomId") { type = NavType.StringType },
                                        navArgument("userId") { type = NavType.StringType },
                                        navArgument("recipientId") { type = NavType.StringType }
                                )
                ) { backStackEntry ->
                    val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""

                    ChatScreen(
                            navController = navController,
                            roomId = roomId,
                            userId = userId,
                            recipientId = recipientId
                    )
                }
            }
        }
    }
}
