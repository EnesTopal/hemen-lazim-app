package com.tpl.hemen_lazim

import android.content.Intent

/** Teklif bildirimine dokununca açılacak sohbet (bildirimin veri alanlarından). */
data class ChatDeepLink(val roomId: String, val otherUserId: String) {

    companion object {
        fun from(intent: Intent?): ChatDeepLink? {
            val roomId = intent?.getStringExtra("roomId") ?: return null
            val otherUserId = intent.getStringExtra("otherUserId") ?: return null
            return ChatDeepLink(roomId, otherUserId)
        }
    }
}
