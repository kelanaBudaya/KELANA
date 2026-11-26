package id.kelana.core.data.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The only wrapper around the platform per-app-locale API, so the repository can be tested with a fake. */
interface AppLocaleController {
    /** BCP 47 tags of the explicitly chosen app locales, comma separated; empty = none chosen yet. */
    fun currentTags(): String

    /** Language tag of the device, used while nothing was chosen. */
    fun deviceTag(): String

    /** Stores the tags as the app locale. Recreates the visible Activity. */
    fun apply(tags: String)
}

@Singleton
class AppCompatLocaleController @Inject constructor() : AppLocaleController {
    override fun currentTags(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    override fun deviceTag(): String = Locale.getDefault().toLanguageTag()

    override fun apply(tags: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }
}
