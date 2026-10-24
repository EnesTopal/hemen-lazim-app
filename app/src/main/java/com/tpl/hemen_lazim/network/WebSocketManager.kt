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
 * Uygulama ömrü boyunca yaşayan tek WebSocket bağlantısı.
 *
 * Bilerek singleton: sohbet ekranına bağlı olsaydı (eskiden öyleydi) sohbet listesi ve
 * alt bardaki okunmamış rozeti canlı sinyal alamazdı — yeni mesaj ancak o odaya girip
 * çıkınca görünürdü.
 *
 * Soket yalnızca CANLI TESLİMAT içindir. Gönderim REST üzerinden yapılır ve okunmamış
 * durumu sunucuda tutulur; dolayısıyla soket kapalıyken de hiçbir şey kaybolmaz, sadece
 * güncelleme anlık olmaz.
 */
object WebSocketManager {

    private const val TAG = "WebSocketManager"
    private const val INITIAL_RECONNECT_DELAY_MS = 2000L
    private const val MAX_RECONNECT_DELAY_MS = 30000L

    private var mStompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob())

    private var currentUrl: String = RetrofitClient.WS_URL
    private var manuallyDisconnected = false
    private var reconnectJob: Job? = null
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS

    // Açık soketin hangi hesap adına kurulduğu. Sunucu kimliği CONNECT anında sabitliyor;
    // aynı cihazda başka hesapla girilince eski soket önceki hesabın mesajlarını getirirdi.
    private var connectedUserId: String? = null

    private val _incomingMessages = MutableSharedFlow<Message>(replay = 0, extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Message> = _incomingMessages

    /** true = bağlandı/yeniden bağlandı, false = koptu. */
    private val _connectionEvents = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 4)
    val connectionEvents: SharedFlow<Boolean> = _connectionEvents

    /**
     * Idempotent: aynı hesap için zaten bağlıysa ya da bağlanmayı deniyorsa hiçbir şey yapmaz.
     * Uygulama öne her geldiğinde güvenle çağrılabilir. Hesap değiştiyse soket yeniden kurulur;
     * bu, oturum token yenileme hatasıyla düştüğünde (çıkış yapılmadan) de geçerli.
     */
    @Synchronized
    fun connect(url: String = RetrofitClient.WS_URL) {
        manuallyDisconnected = false
        currentUrl = url
        if (mStompClient != null && connectedUserId == SharedPreferencesProvider.getUserId()) return
        openConnection(url)
    }

    @Synchronized
    fun disconnect() {
        manuallyDisconnected = true
        reconnectJob?.cancel()
        teardown()
    }

    private fun teardown() {
        // Önce abonelikleri bırak, sonra kapat: aksi halde eski istemcinin CLOSED olayı
        // bize geri döner ve gereksiz bir yeniden bağlanma tetikler.
        compositeDisposable.clear()
        mStompClient?.disconnect()
        mStompClient = null
        connectedUserId = null
    }

    private fun openConnection(url: String) {
        teardown()

        val client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url)
        mStompClient = client

        // ÖNEMLİ: lifecycle() replay'siz bir PublishSubject ve connect() soketi senkron
        // kuruyor. Aboneliği connect()'ten SONRA yapmak, localhost/emülatör gibi hızlı
        // ortamlarda OPENED olayını tamamen kaçırmak demek — bu yüzden önce abone oluyoruz.
        compositeDisposable.add(
                client.lifecycle()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { event ->
                            when (event.type) {
                                LifecycleEvent.Type.OPENED -> {
                                    Log.d(TAG, "STOMP bağlantısı açıldı")
                                    reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
                                    _connectionEvents.tryEmit(true)
                                }
                                LifecycleEvent.Type.ERROR -> {
                                    Log.e(TAG, "STOMP hatası", event.exception)
                                    _connectionEvents.tryEmit(false)
                                    scheduleReconnect()
                                }
                                LifecycleEvent.Type.CLOSED -> {
                                    Log.d(TAG, "STOMP bağlantısı kapandı")
                                    _connectionEvents.tryEmit(false)
                                    scheduleReconnect()
                                }
                                else -> {}
                            }
                        }
        )

        compositeDisposable.add(
                client.topic(USER_QUEUE)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { topicMessage ->
                                    try {
                                        val message =
                                                gson.fromJson(
                                                        topicMessage.payload,
                                                        Message::class.java
                                                )
                                        Log.d(TAG, "Canlı mesaj alındı: id=${message.id}")
                                        _incomingMessages.tryEmit(message)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Mesaj ayrıştırılamadı", e)
                                    }
                                },
                                { error -> Log.e(TAG, "Abonelik hatası", error) }
                        )
        )

        // Token her bağlantı denemesinde yeniden okunur; böylece yeniden bağlanmalar
        // yenilenmiş access token'ı kullanır.
        val token = SharedPreferencesProvider.getAccessToken()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "Token yok, STOMP bağlantısı kurulmuyor")
            teardown()
            return
        }
        connectedUserId = SharedPreferencesProvider.getUserId()
        client.connect(listOf(StompHeader("Authorization", "Bearer $token")))
    }

    private fun scheduleReconnect() {
        if (manuallyDisconnected) return
        reconnectJob?.cancel()
        val delayMs = reconnectDelayMs
        reconnectJob =
                scope.launch {
                    delay(delayMs)
                    reconnectDelayMs = (delayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                    if (!manuallyDisconnected) {
                        Log.d(TAG, "Yeniden bağlanılıyor...")
                        synchronized(WebSocketManager) { openConnection(currentUrl) }
                    }
                }
    }

    private const val USER_QUEUE = "/user/queue/messages"
}
