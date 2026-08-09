package com.tpl.hemen_lazim.model

enum class MessageStatus { SENDING, SENT, FAILED }

/**
 * Hem sunucudan gelen (wire) hem de henüz gönderilmemiş yerel mesajları temsil eder.
 *
 * DİKKAT: [clientId] ve [status] sunucuda YOK; bu alanlar yalnızca istemcide anlamlıdır.
 * Gson, Kotlin'in varsayılan parametre değerlerini uygulamaz (nesneyi Unsafe ile üretir),
 * bu yüzden sunucudan deserialize edilen mesajlarda bu iki alan gerçekten null olur.
 * Tipleri bu yüzden nullable: aksi halde "non-null" ilan edilmiş alanlar runtime'da null
 * taşır ve eşitlik karşılaştırmaları sessizce yanlış sonuç verir.
 */
data class Message(
        val id: Long = 0,
        val roomId: String,
        val senderId: String,
        val recipientId: String,
        val content: String,
        val serverTimestamp: Long = 0,
        val clientId: String? = null,
        val status: MessageStatus? = null
)

/**
 * Liste (LazyColumn) için kararlı anahtar. Sunucuya ulaşmış mesajlar id ile, henüz
 * ulaşmamışlar clientId ile tekilleşir. Aynı mesaj gönderim onayı geldiğinde
 * "loc-" anahtarından "srv-" anahtarına geçer.
 */
val Message.uiKey: String
    get() = if (id != 0L) "srv-$id" else "loc-$clientId"
