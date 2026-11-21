package id.kelana.core.model

/** Pure input rules, identical to the server rules (min 8 characters with letters and digits). */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8

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
