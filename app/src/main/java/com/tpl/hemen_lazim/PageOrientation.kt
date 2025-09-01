package com.tpl.hemen_lazim

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tpl.hemen_lazim.uix.view.Auth
import com.tpl.hemen_lazim.uix.view.MaterialRequest
import com.tpl.hemen_lazim.uix.view.Profile
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider


@Composable
fun PageOrientation(){
    var navController = rememberNavController()
    val context = LocalContext.current
    val token = SharedPreferencesProvider.getToken()
    Log.e("PageOrientation", "Token: $token")

    val startDestination = if (SharedPreferencesProvider.isTokenValidWithToast(context)) "Profile" else "Auth"


    NavHost(navController = navController, startDestination = startDestination){
        composable("Auth") { Auth(navController = navController) }
        composable("Profile") { Profile(navController = navController) }
        composable("MaterialRequest") { MaterialRequest(navController = navController) }

    }
}

/*
Example Data Passing Between Screens
composable("AlarmManagement/{uuid}/{groupName}",
arguments = listOf(
navArgument("uuid") { type = NavType.LongType },
navArgument("groupName") { type = NavType.StringType }
)) {
    val groupId = it.arguments?.getLong("uuid") ?: 0L
    val groupName = it.arguments?.getString("groupName") ?: "Default Group"
    AlarmManagement(navController = navController, groupId = groupId, groupName = groupName) }
*/