package com.tpl.hemen_lazim.uix.preview

import com.tpl.hemen_lazim.model.AuthUiState
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.MaterialFormState
import com.tpl.hemen_lazim.model.Message
import com.tpl.hemen_lazim.model.MessageStatus
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.model.enums.RequestStatus
import com.tpl.hemen_lazim.model.enums.Units
import com.tpl.hemen_lazim.uix.innerview.CreateFormActions
import com.tpl.hemen_lazim.uix.viewmodel.ChatsUiState
import com.tpl.hemen_lazim.uix.viewmodel.MyRequestsUiState
import com.tpl.hemen_lazim.uix.viewmodel.RespondUiState
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/** Önizlemelerdeki örnek veri. Bitiş ve mesaj saatleri önizlemenin çizildiği ana göre. */
internal object PreviewData {
    const val ME = "u-me"
    const val UNREAD_TOTAL = 2
    const val CHAT_PARTNER = "Ayşe Yılmaz"
    const val USER_NAME = "Can Demir"

    private const val AYSE = "u-ayse"
    private const val MEHMET = "u-mehmet"
    private const val ZEYNEP = "u-zeynep"
    private const val LAT = 41.0082
    private const val LNG = 28.9784

    val nearbyRequests = listOf(
        request(
            "n1", "Telefon şarj kablosu (USB-C)", Category.ELEKTRONIK, 1, Units.ADET,
            distanceMeters = 180.0, expiresInMinutes = 45, requesterName = "Mehmet Kaya",
            description = "Telefonum kapanmak üzere, yarım saatliğine ödünç de olur."
        ),
        request("n2", "Yumurta", Category.YEMEK_MALZEMESI, 2, Units.ADET, distanceMeters = 350.0, expiresInMinutes = 80),
        request("n3", "Bebek bezi (4 numara)", Category.HIJYEN, 1, Units.PAKET, distanceMeters = 620.0, expiresInMinutes = 25),
        request("n4", "Bulaşık deterjanı", Category.TEMIZLIK, 1, Units.LITRE, distanceMeters = 940.0, expiresInMinutes = 150),
        request("n5", "Tükenmez kalem", Category.KIRTASIYE, 3, Units.ADET, distanceMeters = 1350.0, expiresInMinutes = 300)
    )

    val explore = RespondUiState(
        radiusKm = 1.5f,
        userLatitude = LAT,
        userLongitude = LNG,
        nearbyRequests = nearbyRequests,
        hasLoaded = true
    )

    val createForm = MaterialFormState(
        title = "Telefon şarj kablosu (USB-C)",
        category = Category.ELEKTRONIK,
        units = Units.ADET,
        latitude = LAT.toString(),
        longitude = LNG.toString()
    )

    /** Önizlemede form alanları değişmez. */
    val formActions = object : CreateFormActions {
        override fun onTitle(v: String) {}
        override fun onDescription(v: String) {}
        override fun onCategory(v: Category) {}
        override fun onQuantity(v: String) {}
        override fun incrementQuantity() {}
        override fun decrementQuantity() {}
        override fun onUnit(v: Units) {}
        override fun updateLocation(latitude: Double, longitude: Double) {}
        override fun onRadiusKm(v: Float) {}
        override fun onDuration(minutes: Int) {}
    }

    val myRequests = MyRequestsUiState(
        requests = listOf(
            request("m1", "Telefon şarj kablosu (USB-C)", Category.ELEKTRONIK, 1, Units.ADET, expiresInMinutes = 135),
            request("m2", "Kalem pil (AA)", Category.ELEKTRONIK, 4, Units.ADET, expiresInMinutes = 35),
            request(
                "m3", "Un", Category.YEMEK_MALZEMESI, 1, Units.KG,
                expiresInMinutes = -600, status = RequestStatus.COMPLETED
            ),
            request(
                "m4", "Çamaşır suyu", Category.TEMIZLIK, 1, Units.LITRE,
                expiresInMinutes = -1500, status = RequestStatus.EXPIRED
            ),
            request(
                "m5", "Islak mendil", Category.HIJYEN, 2, Units.PAKET,
                expiresInMinutes = -4000, status = RequestStatus.CANCELLED
            )
        )
    )

    val chats = ChatsUiState(
        chats = listOf(
            Message(31, "r1", AYSE, ME, "Tamam, 10 dakikaya kapıdayım", at(14, 5)),
            Message(22, "r2", ME, MEHMET, "Çok teşekkürler, çok iyi oldu!", at(19, 40, daysAgo = 1)),
            Message(13, "r3", ZEYNEP, ME, "Kabloyu akşam getirebilirim", at(11, 15, daysAgo = 4))
        ),
        currentUserId = ME,
        userNames = mapOf(AYSE to CHAT_PARTNER, MEHMET to "Mehmet Kaya", ZEYNEP to "Zeynep Arslan")
    )
    val unreadByRoom = mapOf("r1" to UNREAD_TOTAL)

    val chatMessages = listOf(
        Message(1, "r1", AYSE, ME, "Merhaba! USB-C kablom var, işini görür mü?", at(13, 58)),
        Message(2, "r1", ME, AYSE, "Harika, tam da o lazım", at(14, 0)),
        Message(3, "r1", ME, AYSE, "Apartman 12, zile basman yeterli", at(14, 1)),
        Message(4, "r1", AYSE, ME, "Tamam, 10 dakikaya kapıdayım", at(14, 5)),
        Message(
            roomId = "r1", senderId = ME, recipientId = AYSE, content = "Bekliyorum, teşekkürler!",
            serverTimestamp = at(14, 6), clientId = "preview-1", status = MessageStatus.SENDING
        )
    )

    val auth = AuthUiState(username = "can.demir", password = "parola123")

    private fun request(
        id: String,
        title: String,
        category: Category,
        quantity: Int?,
        units: Units?,
        distanceMeters: Double? = null,
        expiresInMinutes: Long,
        status: RequestStatus = RequestStatus.OPEN,
        requesterName: String = CHAT_PARTNER,
        description: String? = null
    ) = MaterialRequestDTO(
        id = id,
        requesterId = "u-$id",
        requesterName = requesterName,
        title = title,
        description = description,
        category = category,
        quantity = quantity,
        units = units,
        latitude = LAT,
        longitude = LNG,
        radiusMeters = 1000,
        status = status,
        expiresAt = Instant.now().plus(Duration.ofMinutes(expiresInMinutes)).toString(),
        createdAt = Instant.now().minus(Duration.ofMinutes(30)).toString(),
        distanceMeters = distanceMeters
    )

    /** Bugün (ya da [daysAgo] gün önce) verilen saatte. */
    private fun at(hour: Int, minute: Int, daysAgo: Long = 0): Long =
        ZonedDateTime.now().minusDays(daysAgo)
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            .toInstant().toEpochMilli()
}
