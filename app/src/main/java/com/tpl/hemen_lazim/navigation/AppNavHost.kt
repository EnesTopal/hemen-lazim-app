package com.tpl.hemen_lazim.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tpl.hemen_lazim.ChatDeepLink
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.session.SessionEvents
import com.tpl.hemen_lazim.ui.AppSnackbar
import com.tpl.hemen_lazim.ui.LocalAppSnackbar
import com.tpl.hemen_lazim.ui.chat.ChatScreen
import com.tpl.hemen_lazim.uix.view.Auth
import com.tpl.hemen_lazim.uix.view.Chats
import com.tpl.hemen_lazim.uix.view.CreateRequest
import com.tpl.hemen_lazim.uix.view.Explore
import com.tpl.hemen_lazim.uix.view.MyRequests
import com.tpl.hemen_lazim.uix.view.Profile
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

/**
 * Kök ekran: tek Scaffold (alt bar yalnızca sekmelerde, uygulama genelinde Snackbar) ve
 * gezinme grafiği. Giriş ekranı ayrı; geri kalan her şey [Routes.MAIN] grafiğinde, böylece
 * oturum girişte de açılsa sekmelerin tabanı hep Keşfet olur.
 */
@Composable
fun AppNavHost(
    startsLoggedIn: Boolean,
    pendingChat: ChatDeepLink?,
    onChatOpened: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val appSnackbar = remember(resources) { AppSnackbar(snackbarHostState, scope, resources) }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val loggedIn = currentRoute != null && currentRoute != Routes.AUTH
    val showBottomBar = currentRoute in TopLevelTab.routes
    val currentOnChatOpened by rememberUpdatedState(onChatOpened)

    if (loggedIn) ChatPresenceEffect()

    // Oturum çıkış yapılmadan düştü (refresh token reddedildi): girişe dön, nedenini söyle
    LaunchedEffect(navController) {
        SessionEvents.expired.collect {
            WebSocketManager.disconnect()
            ChatUnreadStore.clear()
            if (navController.currentDestination?.route != Routes.AUTH) {
                navController.navigateToAuth()
                appSnackbar.show(R.string.session_expired)
            }
        }
    }

    // Bildirimden gelen sohbet: oturum açıksa hemen, değilse girişten sonra açılır
    LaunchedEffect(pendingChat, loggedIn) {
        val link = pendingChat ?: return@LaunchedEffect
        if (!loggedIn) return@LaunchedEffect
        val me = SharedPreferencesProvider.getUserId() ?: return@LaunchedEffect
        navController.navigate(Routes.chat(link.roomId, me, link.otherUserId)) { launchSingleTop = true }
        currentOnChatOpened()
    }

    CompositionLocalProvider(LocalAppSnackbar provides appSnackbar) {
        Scaffold(
            // Ekranlar kendi üst çubuklarını ve kenar boşluklarını yönetiyor
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (showBottomBar) {
                    val unreadByRoom by ChatUnreadStore.unreadByRoom.collectAsStateWithLifecycle()
                    AppBottomBar(currentRoute, unreadByRoom.values.sum()) { tab ->
                        navController.navigateToTab(tab.route)
                    }
                }
            },
            snackbarHost = {
                // Alt bar varken Scaffold zaten onun üstüne koyuyor
                SnackbarHost(
                    snackbarHostState,
                    if (showBottomBar) Modifier else Modifier.navigationBarsPadding().imePadding()
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = if (startsLoggedIn) Routes.MAIN else Routes.AUTH,
                // Alt barın kapladığı alan içteki ekranların Scaffold'larına "kullanıldı" diye
                // bildirilir; yoksa gezinme çubuğu boşluğu iki kez eklenirdi
                modifier = Modifier.padding(padding).consumeWindowInsets(padding)
            ) {
                composable(Routes.AUTH) { Auth(onLoggedIn = { navController.navigateToMain() }) }

                navigation(startDestination = Routes.EXPLORE, route = Routes.MAIN) {
                    composable(Routes.EXPLORE) {
                        Explore(
                            onCreateRequest = { navController.navigate(Routes.CREATE_REQUEST) },
                            onOpenChat = { roomId, me, other ->
                                navController.navigate(Routes.chat(roomId, me, other))
                            }
                        )
                    }
                    composable(Routes.MY_REQUESTS) {
                        MyRequests(onCreateRequest = { navController.navigate(Routes.CREATE_REQUEST) })
                    }
                    composable(Routes.CHATS) { Chats(navController = navController) }
                    composable(Routes.PROFILE) {
                        Profile(
                            onOpenMyRequests = { navController.navigateToTab(Routes.MY_REQUESTS) },
                            onLoggedOut = { navController.navigateToAuth() }
                        )
                    }

                    composable(Routes.CREATE_REQUEST) {
                        CreateRequest(
                            onBack = { navController.popBackStack() },
                            onPublished = {
                                // Önce form kapanır: yoksa İlanlarım sekmesinin kayıtlı yığınına
                                // girip geri tuşunda yeniden açılırdı
                                navController.popBackStack()
                                navController.navigateToTab(Routes.MY_REQUESTS)
                            }
                        )
                    }

                    composable(
                        Routes.CHAT,
                        arguments = listOf(
                            navArgument("roomId") { type = NavType.StringType },
                            navArgument("userId") { type = NavType.StringType },
                            navArgument("recipientId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        ChatScreen(
                            navController = navController,
                            roomId = backStackEntry.arguments?.getString("roomId") ?: "",
                            userId = backStackEntry.arguments?.getString("userId") ?: "",
                            recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
                        )
                    }
                }
            }
        }
    }
}
