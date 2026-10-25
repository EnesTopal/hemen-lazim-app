package com.tpl.hemen_lazim.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Oturum, kullanıcı çıkış yapmadan düştüğünde (refresh token reddedildi) haber verir.
 * Ağ katmanı ekranları bilmediği için yönlendirmeyi kök ekran dinleyerek yapar.
 */
object SessionEvents {

    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
