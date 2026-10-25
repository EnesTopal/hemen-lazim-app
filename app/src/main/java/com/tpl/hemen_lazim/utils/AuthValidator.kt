package com.tpl.hemen_lazim.utils

import androidx.annotation.StringRes
import com.tpl.hemen_lazim.R

object AuthValidator {
    private val EMAIL_REGEX = Regex(
        pattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        option = RegexOption.IGNORE_CASE
    )
    private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")

    // Dönen değer hata metninin strings.xml kimliği; geçerliyse null
    @StringRes
    fun validateEmail(email: String): Int? {
        if (email.isBlank()) return R.string.auth_error_email_required
        return if (EMAIL_REGEX.matches(email)) null else R.string.auth_error_email_invalid
    }

    @StringRes
    fun validatePassword(pass: String): Int? {
        if (pass.isBlank()) return R.string.auth_error_password_required
        return if (PASSWORD_REGEX.matches(pass)) null else R.string.auth_password_rule
    }
}
