package com.tpl.hemen_lazim.uix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.network.repositories.ChatRepository
import com.tpl.hemen_lazim.network.repositories.UserRepository
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
        val error: String? = null,
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
    }

    fun fetchChats(userId: String) =
            viewModelScope.launch {
                _ui.update { it.copy(isLoading = true) }
                try {
                    val chats = chatRepo.getUserChats(userId)
                    _ui.update { it.copy(chats = chats, isLoading = false) }
                    resolveUserNames(chats, userId)
                } catch (e: Exception) {
                    _ui.update { it.copy(isLoading = false, error = e.message) }
                }
            }

    private fun resolveUserNames(chats: List<Message>, currentUserId: String) =
            viewModelScope.launch {
                val otherIds =
                        chats.map { if (it.senderId == currentUserId) it.recipientId else it.senderId }
                                .toSet()
                                .filter { it !in _ui.value.userNames }
                if (otherIds.isEmpty()) return@launch

                val resolved = otherIds.map { id -> async { id to userRepo.getUserName(id) } }.awaitAll()
                _ui.update { it.copy(userNames = it.userNames + resolved) }
            }

    fun refresh() {
        val userId = _ui.value.currentUserId
        if (userId.isNotEmpty()) {
            fetchChats(userId)
        }
    }
}
