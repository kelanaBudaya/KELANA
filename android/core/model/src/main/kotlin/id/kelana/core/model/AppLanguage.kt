package id.kelana.core.model

/** App language choice. [SYSTEM] follows the device language (the default). */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ID("id"),
    EN("en"),
    ;

    companion object {
        /** Maps a stored BCP 47 tag ("" = none) to a choice; unknown languages fall back to [SYSTEM]. */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.substringBefore(',')?.substringBefore('-')?.lowercase().orEmpty()
            return entries.firstOrNull { it != SYSTEM && it.tag == language } ?: SYSTEM
        }
    }
}

/**
 * Per-app language. The choice is kept by the platform (per-app locales), so it survives process death and needs no
 * account. Applying a language recreates the visible Activity, so the UI shows the new language right away.
 */
interface AppLanguageRepository {
    /** The current choice; emits again after [set]. */
    val language: kotlinx.coroutines.flow.Flow<AppLanguage>

    fun set(language: AppLanguage)
}
