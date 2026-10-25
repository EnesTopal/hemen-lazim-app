package com.tpl.hemen_lazim

import android.app.Application
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider

class HemenLazimApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Her bileşenden önce bir kez: bildirim servisi uygulama arka plandayken de oturuma bakıyor
        SharedPreferencesProvider.init(this)
    }
}
