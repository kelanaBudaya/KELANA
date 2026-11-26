package id.kelana.core.data.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

/** The only wrapper around the platform per-app-locale API, so the repository can be tested with a fake. */
interface AppLocaleController {
    /** BCP 47 tags of the app locales, comma separated; empty = follow the system. */
    fun currentTags(): String

    /** Applies the tags ("" = follow the system). Recreates the visible Activity. */
    fun apply(tags: String)
}

@Singleton
class AppCompatLocaleController @Inject constructor() : AppLocaleController {
    override fun currentTags(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    override fun apply(tags: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }
}
