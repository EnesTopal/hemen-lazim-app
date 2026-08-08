package com.tpl.hemen_lazim.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.model.MessageStatus
import com.tpl.hemen_lazim.network.repositories.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository = ChatRepository()) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreHistory = MutableStateFlow(true)
    val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    // Sync/reconnect telafisi için: gördüğümüz en büyük sunucu id'si
    private var lastReceivedId: Long = 0
    private var currentRoomId: String = ""
    private var currentUserId: String = ""
    private var recipientId: String = ""

    init {
        viewModelScope.launch {
            repository.incomingMessages.collect { message ->
                if (message.roomId == currentRoomId) {
                    upsert(message)
                    if (message.id > lastReceivedId) lastReceivedId = message.id
                }
            }
        }
        viewModelScope.launch {
            repository.connectionEvents.collect { connected ->
                // Bağlantı (yeniden) kurulduğunda socket kapalıyken kaçırılmış olabilecek
                // mesajları telafi et. DB zaten tek doğru kaynak, WebSocket sadece sinyal.
                if (connected && currentRoomId.isNotEmpty()) {
                    resync()
                }
            }
        }
    }

    fun initialize(roomId: String, userId: String, otherUserId: String) {
        if (currentRoomId == roomId) return
        currentRoomId = roomId
        currentUserId = userId
        recipientId = otherUserId
        repository.connect()
        loadInitialHistory()
    }

    private fun loadInitialHistory() = viewModelScope.launch {
        try {
            val history = repository.getHistory(currentRoomId, beforeId = null, limit = PAGE_SIZE)
            _hasMoreHistory.value = history.size >= PAGE_SIZE
            history.forEach { upsert(it) }
            lastReceivedId = _messages.value.maxOfOrNull { it.id } ?: 0
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Geçmiş yüklenemedi", e)
        }
    }

    /** Kullanıcı listenin başına yaklaşınca bir önceki sayfayı çeker. */
    fun loadMoreHistory() {
        if (_isLoadingMore.value || !_hasMoreHistory.value) return
        val oldestId = _messages.value.filter { it.id > 0 }.minOfOrNull { it.id } ?: return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val page = repository.getHistory(currentRoomId, beforeId = oldestId, limit = PAGE_SIZE)
                _hasMoreHistory.value = page.size >= PAGE_SIZE
                page.forEach { upsert(it) }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Önceki sayfa yüklenemedi", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun resync() = viewModelScope.launch {
        try {
            val missing = repository.syncMessages(currentRoomId, lastReceivedId)
            missing.forEach {
                upsert(it)
                if (it.id > lastReceivedId) lastReceivedId = it.id
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Resync başarısız", e)
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val pending = Message(
                roomId = currentRoomId,
                senderId = currentUserId,
                recipientId = recipientId,
                content = content,
                status = MessageStatus.SENDING
        )
        upsert(pending)
        viewModelScope.launch { attemptSend(pending) }
    }

    /** Başarısız (FAILED) bir mesajı kullanıcı dokunuşuyla tekrar göndermek için. */
    fun retrySend(failed: Message) {
        if (failed.status != MessageStatus.FAILED) return
        val retryable = failed.copy(status = MessageStatus.SENDING)
        upsert(retryable)
        viewModelScope.launch { attemptSend(retryable) }
    }

    private suspend fun attemptSend(pending: Message) {
        try {
            val saved = repository.sendMessage(pending)
            // clientId'yi koru: bu satır zaten listede duran "pending" ile eşleşsin
            val confirmed = saved.copy(clientId = pending.clientId, status = MessageStatus.SENT)
            upsert(confirmed)
            if (confirmed.id > lastReceivedId) lastReceivedId = confirmed.id
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Mesaj gönderilemedi, FAILED olarak işaretlendi", e)
            upsert(pending.copy(status = MessageStatus.FAILED))
        }
    }

    /**
     * Tek birleştirme noktası: sunucudan gelen (id != 0) mesajlar id'ye göre,
     * henüz sunucuya ulaşmamış yerel mesajlar clientId'ye göre eşleştirilip
     * güncellenir/eklenir. Böylece WebSocket + sync + REST-ack aynı mesajı
     * iki kez göstermez.
     */
    private fun upsert(incoming: Message) {
        val current = _messages.value.toMutableList()
        val idx = current.indexOfFirst {
            (incoming.id != 0L && it.id == incoming.id) || it.clientId == incoming.clientId
        }
        if (idx >= 0) current[idx] = incoming else current.add(incoming)
        _messages.value = current.sortedBy { it.serverTimestamp }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    companion object {
        private const val PAGE_SIZE = 30
    }
}
