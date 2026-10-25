package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.network.ChatApiService
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.network.apiResult
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
    suspend fun sendMessage(message: Message): Message = call {
        apiService.sendMessage(message)
    }

    // Sayfalı geçmiş: beforeId null ise en son "limit" mesaj, değilse ondan öncekiler
    suspend fun getHistory(roomId: String, beforeId: Long? = null, limit: Int = 30): List<Message> = call {
        apiService.getHistory(roomId, beforeId, limit)
    }

    // Bağlantı koptuktan sonra kaçırılan mesajları telafi etmek için
    suspend fun syncMessages(roomId: String, lastReceivedId: Long): List<Message> = call {
        apiService.syncMessages(roomId, lastReceivedId)
    }

    suspend fun createRoom(senderId: String, recipientId: String): String = call {
        apiService.createRoom(senderId, recipientId)["roomId"] ?: error("Room response has no roomId")
    }

    suspend fun getUserChats(userId: String): List<Message> = call {
        apiService.getUserChats(userId)
    }

    // Hata fırlatma davranışı aynı; yalnızca HTTP hataları kodlu ApiException'a çevrilir ki
    // ekranlar metni koddan seçebilsin
    private suspend fun <T> call(block: suspend () -> T): T = apiResult(block).getOrThrow()
}
