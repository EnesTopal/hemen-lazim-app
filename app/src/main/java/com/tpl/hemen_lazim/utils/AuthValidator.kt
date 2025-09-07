package com.tpl.hemen_lazim.utils

object AuthValidator {
    private val EMAIL_REGEX = Regex(
        pattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        option = RegexOption.IGNORE_CASE
    )
    private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")

    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "E-posta zorunlu"
        return if (EMAIL_REGEX.matches(email)) null else "Geçersiz e-posta"
    }

    fun validatePassword(pass: String): String? {
        if (pass.isBlank()) return "Şifre zorunlu"
        return if (PASSWORD_REGEX.matches(pass)) null else "En az 8 karakter, harf + rakam"
    }
}
