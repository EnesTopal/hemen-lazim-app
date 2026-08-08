package com.tpl.hemen_lazim.network

import com.tpl.hemen_lazim.model.Message
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ChatApiService {
    // Güvenilir gönderim yolu: HTTP request/response, socket bağlı olmasa da çalışır
    @POST("/api/chat/messages")
    suspend fun sendMessage(@Body message: Message): Message

    // Sayfalı geçmiş: beforeId yoksa son "limit" mesaj, varsa ondan öncekiler
    @GET("/api/chat/history")
    suspend fun getHistory(
        @Query("roomId") roomId: String,
        @Query("beforeId") beforeId: Long? = null,
        @Query("limit") limit: Int = 30
    ): List<Message>

    @GET("/api/chat/sync")
    suspend fun syncMessages(
        @Query("roomId") roomId: String,
        @Query("lastReceivedMessageId") lastReceivedMessageId: Long
    ): List<Message>

    @POST("/api/chat/room")
    suspend fun createRoom(
        @Query("senderId") senderId: String,
        @Query("recipientId") recipientId: String
    ): Map<String, String>

    @GET("/api/chat/rooms")
    suspend fun getUserChats(
        @Query("userId") userId: String
    ): List<Message>
}
