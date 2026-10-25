package com.tpl.hemen_lazim.ui

import androidx.annotation.StringRes
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.network.ApiCodes
import com.tpl.hemen_lazim.network.ApiException
import java.io.IOException

/**
 * Hatanın kullanıcıya gösterilecek metni (strings.xml, telefonun dilinde). Sunucunun kodu
 * biliniyorsa ona özel metin, bilinmiyorsa HTTP durumuna göre genel bir metin; sunucunun
 * İngilizce mesajı hiçbir zaman gösterilmez.
 */
@StringRes
fun Throwable.messageRes(): Int = when (this) {
    is ApiException -> code?.let(::codeMessageRes) ?: statusMessageRes(status)
    is IOException -> R.string.error_network
    else -> R.string.error_unknown
}

@StringRes
private fun codeMessageRes(code: String): Int? = when (code) {
    ApiCodes.VALIDATION_FAILED -> R.string.error_validation
    ApiCodes.REQUEST_NOT_FOUND -> R.string.error_request_not_found
    ApiCodes.REQUEST_NOT_OPEN -> R.string.error_request_not_open
    ApiCodes.NOT_REQUEST_OWNER -> R.string.error_not_request_owner
    ApiCodes.OFFER_ON_OWN_REQUEST -> R.string.error_offer_on_own_request
    ApiCodes.USER_NOT_FOUND -> R.string.error_user_not_found
    ApiCodes.CHAT_WITH_SELF -> R.string.error_chat_with_self
    ApiCodes.NOT_CHAT_MEMBER -> R.string.error_not_chat_member
    ApiCodes.MESSAGE_EMPTY -> R.string.error_message_empty
    ApiCodes.MESSAGE_TOO_LONG -> R.string.error_message_too_long
    ApiCodes.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
    ApiCodes.ACCOUNT_DISABLED -> R.string.error_account_disabled
    ApiCodes.ACCOUNT_LOCKED -> R.string.error_account_locked
    ApiCodes.AUTHENTICATION_FAILED -> R.string.error_authentication_failed
    ApiCodes.USERNAME_INVALID -> R.string.error_username_invalid
    ApiCodes.EMAIL_INVALID -> R.string.auth_error_email_invalid
    ApiCodes.PASSWORD_INVALID -> R.string.auth_password_rule
    ApiCodes.PASSWORD_TOO_LONG -> R.string.error_password_too_long
    ApiCodes.USERNAME_TAKEN -> R.string.error_username_taken
    ApiCodes.EMAIL_TAKEN -> R.string.error_email_taken
    ApiCodes.UNAUTHORIZED,
    ApiCodes.REFRESH_NOT_FOUND,
    ApiCodes.REFRESH_REVOKED,
    ApiCodes.REFRESH_EXPIRED -> R.string.session_expired
    // Diğerleri (INVALID_USER_ID, BAD_REQUEST, NOT_FOUND...) durum koduna göre
    else -> null
}

@StringRes
private fun statusMessageRes(status: Int): Int = when {
    status == 401 -> R.string.session_expired
    status == 403 -> R.string.error_forbidden
    status == 404 -> R.string.error_not_found
    status == 409 -> R.string.error_conflict
    status >= 500 -> R.string.error_server
    else -> R.string.error_request_failed
}
