package com.tpl.hemen_lazim.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/** Uygulamadaki tüm hedefler tek yerde. */
object Routes {
    const val AUTH = "auth"

    /** Giriş yapmış kullanıcının iç içe grafiği; başlangıcı Keşfet. */
    const val MAIN = "main"

    // Alt bardaki sekmeler
    const val EXPLORE = "explore"
    const val MY_REQUESTS = "my_requests"
    const val CHATS = "chats"
    const val PROFILE = "profile"

    // Sekme dışı tam ekranlar (alt bar gizli)
    const val CREATE_REQUEST = "create_request"
    const val CHAT = "chat/{roomId}/{userId}/{recipientId}"

    fun chat(roomId: String, userId: String, recipientId: String) = "chat/$roomId/$userId/$recipientId"
}

/**
 * Sekmeler arası standart geçiş: her sekme kendi yığınını ve kaydırma/harita durumunu korur,
 * geri tuşu önce Keşfet'e döner, Keşfet'ten uygulamadan çıkar.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.EXPLORE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Oturum bitti (çıkış ya da süre doldu): girişe dön, geride hiçbir ekran kalmasın. */
fun NavController.navigateToAuth() {
    navigate(Routes.AUTH) { clearAll(this@navigateToAuth) }
}

/** Giriş yapıldı: ana grafiğe geç, giriş ekranı geri yığında kalmasın. */
fun NavController.navigateToMain() {
    navigate(Routes.MAIN) { clearAll(this@navigateToMain) }
}

private fun NavOptionsBuilder.clearAll(navController: NavController) {
    popUpTo(navController.graph.id) { inclusive = true }
    launchSingleTop = true
}
