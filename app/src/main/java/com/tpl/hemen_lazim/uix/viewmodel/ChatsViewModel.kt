package com.tpl.hemen_lazim.uix.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.network.WebSocketManager
import com.tpl.hemen_lazim.network.repositories.ChatRepository
import com.tpl.hemen_lazim.network.repositories.ChatUnreadStore
import com.tpl.hemen_lazim.network.repositories.UserRepository
import com.tpl.hemen_lazim.ui.messageRes
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatsUiState(
        val chats: List<Message> = emptyList(),
        val isLoading: Boolean = false,
        @StringRes val error: Int? = null,
        val currentUserId: String = "",
        val userNames: Map<String, String> = emptyMap()
)

class ChatsViewModel(
        private val chatRepo: ChatRepository = ChatRepository(),
        private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(ChatsUiState())
    val ui = _ui.asStateFlow()

    init {
        val userId = SharedPreferencesProvider.getUserId()
        if (userId != null) {
            _ui.update { it.copy(currentUserId = userId) }
            fetchChats(userId)
        }
        // Canlı mesaj geldiğinde listeyi tazele: aksi halde yeni mesaj ancak odaya
        // girip çıkınca listede görünürdü.
        viewModelScope.launch {
            WebSocketManager.incomingMessages.collect { refresh() }
        }
    }

    fun fetchChats(userId: String) =
            viewModelScope.launch {
                _ui.update { it.copy(isLoading = true) }
                try {
                    val chats = chatRepo.getUserChats(userId)
                    _ui.update { it.copy(chats = chats, isLoading = false, error = null) }
                    resolveUserNames(chats, userId)
                } catch (e: Exception) {
                    _ui.update { it.copy(isLoading = false, error = e.messageRes()) }
                }
            }

    private fun resolveUserNames(chats: List<Message>, currentUserId: String) =
            viewModelScope.launch {
                val otherIds =
                        chats.map { if (it.senderId == currentUserId) it.recipientId else it.senderId }
                                .toSet()
                                .filter { it !in _ui.value.userNames }
                if (otherIds.isEmpty()) return@launch

                // Alınamayan isim kaydedilmez: listede "…" görünür, sonraki tazelemede yeniden denenir
                val resolved =
                        otherIds.map { id -> async { userRepo.getUserName(id)?.let { id to it } } }
                                .awaitAll()
                                .filterNotNull()
                _ui.update { it.copy(userNames = it.userNames + resolved) }
            }

    fun refresh() {
        val userId = _ui.value.currentUserId
        if (userId.isNotEmpty()) {
            fetchChats(userId)
        }
        ChatUnreadStore.refresh()
    }
}
