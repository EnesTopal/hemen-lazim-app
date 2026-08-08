package com.tpl.hemen_lazim.uix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.network.repositories.ChatRepository
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatsUiState(
        val chats: List<Message> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val currentUserId: String = ""
)

class ChatsViewModel(private val chatRepo: ChatRepository = ChatRepository()) : ViewModel() {

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
                } catch (e: Exception) {
                    _ui.update { it.copy(isLoading = false, error = e.message) }
                }
            }

    fun refresh() {
        val userId = _ui.value.currentUserId
        if (userId.isNotEmpty()) {
            fetchChats(userId)
        }
    }
}
