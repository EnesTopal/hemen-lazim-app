package com.tpl.hemen_lazim

import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bildirimden gelen ve henüz açılmamış sohbet; ekran dönse de kaybolmaz. */
class MainViewModel : ViewModel() {

    private val _pendingChat = MutableStateFlow<ChatDeepLink?>(null)
    val pendingChat: StateFlow<ChatDeepLink?> = _pendingChat.asStateFlow()

    fun onIntent(intent: Intent?) {
        ChatDeepLink.from(intent)?.let { _pendingChat.value = it }
    }

    fun onChatOpened() {
        _pendingChat.value = null
    }
}
