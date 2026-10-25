package com.tpl.hemen_lazim.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.model.MessageStatus
import com.tpl.hemen_lazim.network.repositories.ChatRepository
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.network.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
        private val repository: ChatRepository = ChatRepository(),
        private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreHistory = MutableStateFlow(true)
    val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    private val _otherUserName = MutableStateFlow("")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()

    /** Soket bağlı mı: ekran, emniyet yoklamasını buna göre seyrekleştirir. */
    val isConnected: StateFlow<Boolean> =
            repository.connectionEvents.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Sync/reconnect telafisi için: gördüğümüz en büyük sunucu id'si
    private var lastReceivedId: Long = 0
    private var currentRoomId: String = ""
    private var currentUserId: String = ""
    private var recipientId: String = ""

    init {
        viewModelScope.launch {
            repository.incomingMessages.collect { message ->
                if (message.roomId == currentRoomId) {
                    merge(listOf(message))
                    if (message.id > lastReceivedId) lastReceivedId = message.id
                    ChatUnreadStore.markRoomRead(currentRoomId)
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
        _otherUserName.value = otherUserId
        repository.connect()
        // Bu oda ekranda açık: gelen mesajlar okunmamış sayılmasın, bekleyenler temizlensin
        ChatUnreadStore.setActiveRoom(roomId)
        loadInitialHistory()
        viewModelScope.launch { _otherUserName.value = userRepository.getUserName(otherUserId) }
    }

    private fun loadInitialHistory() =
            viewModelScope.launch {
                try {
                    val history =
                            repository.getHistory(
                                    currentRoomId,
                                    beforeId = null,
                                    limit = PAGE_SIZE
                            )
                    _hasMoreHistory.value = history.size >= PAGE_SIZE
                    merge(history)
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
                val page =
                        repository.getHistory(
                                currentRoomId,
                                beforeId = oldestId,
                                limit = PAGE_SIZE
                        )
                _hasMoreHistory.value = page.size >= PAGE_SIZE
                merge(page)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Önceki sayfa yüklenemedi", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * Kaçırılmış mesajları sunucudan tamamlar. Hem yeniden bağlanınca hem de ekran
     * açıkken periyodik olarak çağrılır: canlı teslimat WebSocket'e bağlı olduğu için
     * soketin sessizce düşmesi durumunda ekranın donmaması gerekiyor. Sunucu bu isteği
     * üst sınırla yanıtlıyor, dolayısıyla lastReceivedId=0 olsa bile güvenli.
     */
    fun resync() =
            viewModelScope.launch {
                if (currentRoomId.isEmpty()) return@launch
                try {
                    val missing = repository.syncMessages(currentRoomId, lastReceivedId)
                    if (missing.isEmpty()) return@launch
                    merge(missing)
                    missing.forEach { if (it.id > lastReceivedId) lastReceivedId = it.id }
                    // Ekran açıkken gelen mesajlar okunmuş sayılır
                    ChatUnreadStore.markRoomRead(currentRoomId)
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Resync başarısız", e)
                }
            }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val pending =
                Message(
                        roomId = currentRoomId,
                        senderId = currentUserId,
                        recipientId = recipientId,
                        content = content,
                        // Sunucu onayı gelene kadar listenin en altında dursun diye
                        // yerel zaman damgası veriyoruz; onay gelince gerçeğiyle değişir.
                        serverTimestamp = System.currentTimeMillis(),
                        clientId = UUID.randomUUID().toString(),
                        status = MessageStatus.SENDING
                )
        merge(listOf(pending))
        viewModelScope.launch { attemptSend(pending) }
    }

    /** Başarısız (FAILED) bir mesajı kullanıcı dokunuşuyla tekrar göndermek için. */
    fun retrySend(failed: Message) {
        if (failed.status != MessageStatus.FAILED) return
        val retryable = failed.copy(status = MessageStatus.SENDING)
        merge(listOf(retryable))
        viewModelScope.launch { attemptSend(retryable) }
    }

    private suspend fun attemptSend(pending: Message) {
        try {
            val saved = repository.sendMessage(pending)
            // clientId'yi koru: bu satır zaten listede duran "pending" ile eşleşsin
            val confirmed = saved.copy(clientId = pending.clientId, status = MessageStatus.SENT)
            merge(listOf(confirmed))
            if (confirmed.id > lastReceivedId) lastReceivedId = confirmed.id
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Mesaj gönderilemedi, FAILED olarak işaretlendi", e)
            merge(listOf(pending.copy(status = MessageStatus.FAILED)))
        }
    }

    /**
     * Tek birleştirme noktası: WebSocket push'u, REST geçmişi, sync ve gönderim onayı
     * hep buradan geçer, böylece aynı mesaj listede iki kez görünmez.
     */
    private fun merge(incoming: List<Message>) {
        if (incoming.isEmpty()) return
        val current = _messages.value.toMutableList()
        incoming.forEach { msg ->
            val idx = current.indexOfFirst { isSameMessage(it, msg) }
            if (idx >= 0) current[idx] = msg else current.add(msg)
        }
        // serverTimestamp aynı milisaniyeye düşen mesajlar için id ikinci ölçüt
        _messages.value = current.sortedWith(compareBy({ it.serverTimestamp }, { it.id }))
    }

    /**
     * İki mesaj aynı mesaj mı? Sunucuya ulaşmış olanlar id ile, henüz ulaşmamış yerel
     * olanlar clientId ile eşleşir.
     *
     * clientId sunucuda tutulmuyor, dolayısıyla sunucudan gelen her mesajda null.
     * "null == null" eşleşmesine izin verilirse tüm geçmiş tek bir satıra çöker.
     */
    private fun isSameMessage(a: Message, b: Message): Boolean {
        if (a.id != 0L && b.id != 0L) return a.id == b.id
        val ac = a.clientId
        return ac != null && ac == b.clientId
    }

    override fun onCleared() {
        super.onCleared()
        // Soket bilerek kapatılmıyor: uygulama ömrüne ait ve sohbet listesi ile
        // alt bardaki rozet bu ekran kapandıktan sonra da canlı sinyale ihtiyaç duyuyor.
        ChatUnreadStore.setActiveRoom(null)
    }

    companion object {
        private const val PAGE_SIZE = 50
    }
}
