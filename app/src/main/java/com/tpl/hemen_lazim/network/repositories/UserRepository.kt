package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.UserApiService
import java.util.concurrent.ConcurrentHashMap

/**
 * Kullanıcı adları neredeyse hiç değişmez; ekranlar arasında tekrar tekrar
 * ağ isteği atmamak için basit, uygulama ömrü boyunca yaşayan bir bellek içi önbellek.
 */
class UserRepository {
    private val apiService = RetrofitClient.retrofit.create(UserApiService::class.java)

    suspend fun getUserName(userId: String): String {
        nameCache[userId]?.let { return it }
        return try {
            val user = apiService.getUser(userId)
            nameCache[userId] = user.userName
            user.userName
        } catch (e: Exception) {
            userId
        }
    }

    companion object {
        private val nameCache = ConcurrentHashMap<String, String>()
    }
}
