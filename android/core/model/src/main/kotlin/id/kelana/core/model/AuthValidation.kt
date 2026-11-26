package id.kelana.core.model

/** Pure input rules, identical to the server rules (min 8 characters with letters and digits). */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_DISPLAY_NAME_LENGTH = 40

    /** Trims the nickname; blank means "not set" (null). Length is checked separately by [isValidDisplayName]. */
    fun normalizeDisplayName(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

    /** A set name must be 1..[MAX_DISPLAY_NAME_LENGTH] characters after trimming. */
    fun isValidDisplayName(raw: String): Boolean = raw.trim().length in 1..MAX_DISPLAY_NAME_LENGTH

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean {
        val value = email.trim()
        return value.length <= 254 && emailRegex.matches(value) && !value.contains("..")
    }

    fun isValidPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH &&
            password.any { it in 'A'..'Z' || it in 'a'..'z' } &&
            password.any { it in '0'..'9' }
}
