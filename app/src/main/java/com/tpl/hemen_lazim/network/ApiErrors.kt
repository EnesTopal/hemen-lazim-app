package com.tpl.hemen_lazim.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response

/**
 * Sunucunun döndüğü hata. Kullanıcıya gösterilecek metin [code]'dan seçilir
 * (ui/ErrorMessages.kt, telefonun dilinde); sunucunun İngilizce mesajı yalnızca log ve hata
 * ayıklama için [message]'da durur. Kod yoksa (eski sunucu, okunamayan gövde) [status]'a bakılır.
 */
class ApiException(val code: String?, val status: Int, message: String?) : Exception(message)

/** Sunucunun sabit kodları (sunucu: errors/ErrorCode.java ve ApiResponse.code). Adlar sözleşmedir. */
object ApiCodes {
    const val VALIDATION_FAILED = "VALIDATION_FAILED"
    const val UNAUTHORIZED = "UNAUTHORIZED"

    const val REQUEST_NOT_FOUND = "REQUEST_NOT_FOUND"
    const val REQUEST_NOT_OPEN = "REQUEST_NOT_OPEN"
    const val NOT_REQUEST_OWNER = "NOT_REQUEST_OWNER"
    const val OFFER_ON_OWN_REQUEST = "OFFER_ON_OWN_REQUEST"

    const val USER_NOT_FOUND = "USER_NOT_FOUND"
    const val CHAT_WITH_SELF = "CHAT_WITH_SELF"
    const val NOT_CHAT_MEMBER = "NOT_CHAT_MEMBER"
    const val MESSAGE_EMPTY = "MESSAGE_EMPTY"
    const val MESSAGE_TOO_LONG = "MESSAGE_TOO_LONG"

    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val ACCOUNT_DISABLED = "ACCOUNT_DISABLED"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
    const val USERNAME_INVALID = "USERNAME_INVALID"
    const val EMAIL_INVALID = "EMAIL_INVALID"
    const val PASSWORD_INVALID = "PASSWORD_INVALID"
    const val PASSWORD_TOO_LONG = "PASSWORD_TOO_LONG"
    const val USERNAME_TAKEN = "USERNAME_TAKEN"
    const val EMAIL_TAKEN = "EMAIL_TAKEN"

    const val REFRESH_NOT_FOUND = "REFRESH_NOT_FOUND"
    const val REFRESH_REVOKED = "REFRESH_REVOKED"
    const val REFRESH_EXPIRED = "REFRESH_EXPIRED"

    // Başarılı yanıttaki sonuç kodları
    const val REQUESTER_ALREADY_NOTIFIED = "REQUESTER_ALREADY_NOTIFIED"
}

// Alan adları R8 küçültmesinde de korunsun (Gson yansımayla okuyor)
private class ErrorBody(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?
)

private val gson = Gson()

/** Başarılıysa gövde (boş olabilir), değilse sunucunun kodlu hatası fırlatılır. */
fun <T> Response<T>.bodyOrThrow(): T? {
    if (!isSuccessful) throw toApiException()
    return body()
}

/** Hata gövdesi {"code": "...", "message": "..."}; okunamazsa yalnızca HTTP durumu kalır. */
fun Response<*>.toApiException(): ApiException {
    val raw = runCatching { errorBody()?.string() }.getOrNull()
    val body = runCatching { gson.fromJson(raw, ErrorBody::class.java) }.getOrNull()
    return ApiException(
        code = body?.code?.takeIf { it.isNotBlank() },
        status = code(),
        message = body?.message?.takeIf { it.isNotBlank() } ?: raw?.take(200)
    )
}

/**
 * Ağ çağrısını Result'a çevirir: bağlantı hatası IOException olarak, HTTP hatası kodlu
 * ApiException olarak döner. Coroutine iptali yutulmaz, yukarı iletilir.
 */
suspend fun <T> apiResult(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        // Gövde dönen (Response sarmalamayan) Retrofit çağrıları HTTP hatasında bunu fırlatıyor
        Result.failure(e.response()?.toApiException() ?: ApiException(null, e.code(), e.message()))
    } catch (e: Exception) {
        Result.failure(e)
    }
