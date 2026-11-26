package id.kelana.core.data.language

import id.kelana.core.model.AppLanguage
import id.kelana.core.model.AppLanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlatformAppLanguageRepository @Inject constructor(
    private val controller: AppLocaleController,
) : AppLanguageRepository {
    private var explicit = controller.currentTags().isNotEmpty()
    private val state = MutableStateFlow(
        AppLanguage.fromTag(controller.currentTags().ifEmpty { controller.deviceTag() }),
    )

    override val language: Flow<AppLanguage> = state.asStateFlow()

    override fun set(language: AppLanguage) {
        // Picking the language that is already shown still stores it, so a later device change does not move the app.
        if (explicit && state.value == language) return
        explicit = true
        state.value = language
        controller.apply(language.tag)
    }
}
