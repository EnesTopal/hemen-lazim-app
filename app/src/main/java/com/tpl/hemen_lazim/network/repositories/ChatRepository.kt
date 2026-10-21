package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.network.ChatApiService
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.WebSocketManager
import kotlinx.coroutines.flow.SharedFlow

class ChatRepository {
    private val apiService = RetrofitClient.retrofit.create(ChatApiService::class.java)

    // Soket uygulama ömrüne ait; burada oluşturulmuyor, yalnızca kullanılıyor
    val incomingMessages: SharedFlow<Message> get() = WebSocketManager.incomingMessages
    val connectionEvents: SharedFlow<Boolean> get() = WebSocketManager.connectionEvents

    fun connect() {
        WebSocketManager.connect()
    }

    // Güvenilir gönderim: REST üzerinden, sunucunun atadığı gerçek id + serverTimestamp ile döner
    suspend fun sendMessage(message: Message): Message {
        return apiService.sendMessage(message)
    }

    // Sayfalı geçmiş: beforeId null ise en son "limit" mesaj, değilse ondan öncekiler
    suspend fun getHistory(roomId: String, beforeId: Long? = null, limit: Int = 30): List<Message> {
        return apiService.getHistory(roomId, beforeId, limit)
    }

    // Bağlantı koptuktan sonra kaçırılan mesajları telafi etmek için
    suspend fun syncMessages(roomId: String, lastReceivedId: Long): List<Message> {
        return apiService.syncMessages(roomId, lastReceivedId)
    }

    suspend fun createRoom(senderId: String, recipientId: String): String {
        val response = apiService.createRoom(senderId, recipientId)
        return response["roomId"] ?: throw Exception("Room ID not found in response")
    }

    suspend fun getUserChats(userId: String): List<Message> {
        return apiService.getUserChats(userId)
    }
}
