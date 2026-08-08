package com.tpl.hemen_lazim.model

import java.util.UUID

enum class MessageStatus { SENDING, SENT, FAILED }

data class Message(
    val id: Long = 0,
    val roomId: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val serverTimestamp: Long = 0,
    val clientId: String = UUID.randomUUID().toString(),
    val status: MessageStatus = MessageStatus.SENT
)
