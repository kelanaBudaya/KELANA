package id.kelana.core.model

/** The two app languages. There is no "system" choice: until one is picked, the language the app shows is used. */
enum class AppLanguage(val tag: String) {
    ID("id"),
    EN("en"),
    ;

    companion object {
        /**
         * Maps a BCP 47 tag to the language the app shows: English for "en", Indonesian for anything else
         * (the default resources in `values/` are Indonesian).
         */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.substringBefore(',')?.substringBefore('-')?.lowercase().orEmpty()
            return if (language == EN.tag) EN else ID
        }
    }
}

/**
 * Per-app language. The choice is kept by the platform (per-app locales), so it survives process death and needs no
 * account. Applying a language recreates the visible Activity, so the UI shows the new language right away.
 */
interface AppLanguageRepository {
    /** The language the app currently shows: the explicit choice, or the device language while nothing was chosen. */
    val language: kotlinx.coroutines.flow.Flow<AppLanguage>

    /** Stores an explicit language. */
    fun set(language: AppLanguage)
}
