package com.tpl.hemen_lazim

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tpl.hemen_lazim.network.repositories.AuthRepository
import com.tpl.hemen_lazim.ui.chat.ChatScreen
import com.tpl.hemen_lazim.uix.view.Auth
import com.tpl.hemen_lazim.uix.view.Chats
import com.tpl.hemen_lazim.uix.view.MaterialRequest
import com.tpl.hemen_lazim.uix.view.Profile
import com.tpl.hemen_lazim.uix.view.Splash
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

@Composable
fun PageOrientation(deepLinkRoomId: String? = null, deepLinkOtherUserId: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "Splash") {
        composable("Splash") {
            val repo = remember { AuthRepository() }
            Splash(navController = navController, repo = repo)

            // Handle Deep Link after Splash (or check token first)
            LaunchedEffect(deepLinkRoomId, deepLinkOtherUserId) {
                if (deepLinkRoomId != null && deepLinkOtherUserId != null) {
                    val currentUserId = SharedPreferencesProvider.getUserId()
                    if (currentUserId != null) {
                        navController.navigate(
                                "Chat/$deepLinkRoomId/$currentUserId/$deepLinkOtherUserId"
                        )
                    }
                }
            }
        }
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
