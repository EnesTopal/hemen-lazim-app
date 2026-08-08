package com.tpl.hemen_lazim.network

import android.util.Log
import com.google.gson.Gson
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader

/**
 * WebSocket burada sadece CANLI TESLİMAT içindir (karşı taraf o an bağlıysa anlık düşer).
 * Mesaj GÖNDERİMİ artık REST üzerinden (ChatRepository.sendMessage) yapılır; socket'in
 * kapalı olması gönderimi etkilemez, sadece anlık teslimatı geciktirir — bağlantı
 * kurulduğunda/yeniden kurulduğunda [connectionEvents] tetiklenir ve çağıran taraf
 * bunu bir "sync" ile telafi eder.
 */
class WebSocketManager {
    private var mStompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob())

    private var currentUrl: String? = null
    private var manuallyDisconnected = false
    private var reconnectJob: Job? = null
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS

    private val _incomingMessages = MutableSharedFlow<Message>(replay = 0, extraBufferCapacity = 16)
    val incomingMessages: SharedFlow<Message> = _incomingMessages

    // true = bağlandı/yeniden bağlandı, false = koptu
    private val _connectionEvents = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 4)
    val connectionEvents: SharedFlow<Boolean> = _connectionEvents

    fun connect(url: String) {
        manuallyDisconnected = false
        currentUrl = url
        if (mStompClient != null && mStompClient!!.isConnected) return
        openConnection(url)
    }

    private fun openConnection(url: String) {
        val client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url)
        mStompClient = client

        val token = SharedPreferencesProvider.getAccessToken()
        val headers = if (!token.isNullOrBlank()) {
            listOf(StompHeader("Authorization", "Bearer $token"))
        } else {
            emptyList()
        }
        client.connect(headers)

        val lifecycleDisposable = client.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d("WebSocket", "Stomp connection opened")
                        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
                        _connectionEvents.tryEmit(true)
                    }
                    LifecycleEvent.Type.ERROR -> {
                        Log.e("WebSocket", "Error", lifecycleEvent.exception)
                        _connectionEvents.tryEmit(false)
                        scheduleReconnect()
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        Log.d("WebSocket", "Stomp connection closed")
                        _connectionEvents.tryEmit(false)
                        scheduleReconnect()
                    }
                    else -> {}
                }
            }
        compositeDisposable.add(lifecycleDisposable)

        // Subscribe to user queue
        val topicDisposable = client.topic("/user/queue/messages")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                try {
                    val message = gson.fromJson(topicMessage.payload, Message::class.java)
                    _incomingMessages.tryEmit(message)
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing message", e)
                }
            }, { error ->
                Log.e("WebSocket", "Error on subscribe", error)
            })
        compositeDisposable.add(topicDisposable)
    }

    private fun scheduleReconnect() {
        if (manuallyDisconnected) return
        val url = currentUrl ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            if (!manuallyDisconnected) {
                openConnection(url)
            }
        }
    }

    fun disconnect() {
        manuallyDisconnected = true
        reconnectJob?.cancel()
        mStompClient?.disconnect()
        compositeDisposable.clear()
        mStompClient = null
    }

    companion object {
        private const val INITIAL_RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
    }
}
