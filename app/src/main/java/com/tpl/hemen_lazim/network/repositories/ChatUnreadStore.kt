package com.tpl.hemen_lazim.network.repositories

import android.util.Log
import com.tpl.hemen_lazim.network.ChatApiService
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Okunmamış mesaj rozetlerinin tek kaynağı (alt bar + sohbet listesi).
 *
 * Gerçek doğru kaynak sunucudur ([refresh]); WebSocket yalnızca sayacı anında
 * artırmak için kullanılır. Bu ayrım kasıtlı: rozet uygulama kapalıyken gelen
 * mesajları da göstermek zorunda, o yüzden sayaç istemci belleğinde tutulamaz.
 */
object ChatUnreadStore {

    private const val TAG = "ChatUnreadStore"

    private val api = RetrofitClient.retrofit.create(ChatApiService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _unreadByRoom = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadByRoom: StateFlow<Map<String, Int>> = _unreadByRoom.asStateFlow()

    /** O an ekranda açık olan oda; oraya gelen mesaj okunmamış sayılmaz. */
    private var activeRoomId: String? = null

    init {
        scope.launch {
            WebSocketManager.incomingMessages.collect { message ->
                val me = SharedPreferencesProvider.getUserId()
                // Kendi gönderdiğimiz mesaj okunmamış sayılmaz
                if (me == null || message.recipientId != me) return@collect

                if (message.roomId == activeRoomId) {
                    // Kullanıcı zaten o odaya bakıyor: anında okundu say
                    markRoomRead(message.roomId)
                } else {
                    _unreadByRoom.update { current ->
                        current + (message.roomId to ((current[message.roomId] ?: 0) + 1))
                    }
                }
            }
        }
    }

    /** Uygulama öne geldiğinde ve sohbet listesi açıldığında çağrılır. */
    fun refresh() {
        if (SharedPreferencesProvider.getAccessToken().isNullOrBlank()) return
        scope.launch {
            try {
                _unreadByRoom.value = api.getUnreadCounts()
            } catch (e: Exception) {
                Log.e(TAG, "Okunmamış sayıları alınamadı", e)
            }
        }
    }

    /** Sohbet ekranı açılırken/kapanırken bildirilir. */
    fun setActiveRoom(roomId: String?) {
        activeRoomId = roomId
        if (roomId != null) markRoomRead(roomId)
    }

    fun markRoomRead(roomId: String) {
        // Önce yereli düşür ki rozet anında kaybolsun, sonra sunucuya bildir.
        _unreadByRoom.update { it - roomId }
        scope.launch {
            try {
                api.markRoomRead(roomId)
            } catch (e: Exception) {
                Log.e(TAG, "Oda okundu işaretlenemedi: $roomId", e)
            }
        }
    }
}
