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
    private val state = MutableStateFlow(AppLanguage.fromTag(controller.currentTags()))

    override val language: Flow<AppLanguage> = state.asStateFlow()

    override fun set(language: AppLanguage) {
        if (state.value == language) return
        state.value = language
        controller.apply(language.tag)
    }
}
