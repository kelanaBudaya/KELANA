package id.kelana.core.data.language

import app.cash.turbine.test
import id.kelana.core.model.AppLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformAppLanguageRepositoryTest {
    private class FakeController(var tags: String = "", val device: String = "id-ID") : AppLocaleController {
        val applied = mutableListOf<String>()
        override fun currentTags() = tags
        override fun deviceTag() = device
        override fun apply(tags: String) {
            this.tags = tags
            applied += tags
        }
    }

    private suspend fun first(controller: FakeController): AppLanguage {
        var result = AppLanguage.ID
        PlatformAppLanguageRepository(controller).language.test { result = awaitItem() }
        return result
    }

    @Test
    fun explicitChoiceWinsOverDevice() = runTest {
        assertEquals(AppLanguage.EN, first(FakeController(tags = "en-US", device = "id-ID")))
        assertEquals(AppLanguage.ID, first(FakeController(tags = "id", device = "en-US")))
    }

    @Test
    fun withoutChoice_showsTheResolvedDeviceLanguage() = runTest {
        assertEquals(AppLanguage.ID, first(FakeController(device = "id-ID")))
        assertEquals(AppLanguage.EN, first(FakeController(device = "en-GB")))
        // Other device languages fall back to the default (Indonesian) resources.
        assertEquals(AppLanguage.ID, first(FakeController(device = "fr-FR")))
    }

    @Test
    fun setAppliesTagAndEmits() = runTest {
        val controller = FakeController(device = "id-ID")
        val repo = PlatformAppLanguageRepository(controller)
        repo.language.test {
            assertEquals(AppLanguage.ID, awaitItem())
            repo.set(AppLanguage.EN)
            assertEquals(AppLanguage.EN, awaitItem())
            repo.set(AppLanguage.ID)
            assertEquals(AppLanguage.ID, awaitItem())
        }
        assertEquals(listOf("en", "id"), controller.applied)
    }

    @Test
    fun choosingTheShownLanguageStillStoresItOnce() {
        val controller = FakeController(device = "id-ID")
        val repo = PlatformAppLanguageRepository(controller)
        repo.set(AppLanguage.ID)
        repo.set(AppLanguage.ID)
        assertEquals(listOf("id"), controller.applied)
    }

    @Test
    fun settingTheStoredLanguageDoesNotReapply() {
        val controller = FakeController(tags = "en")
        PlatformAppLanguageRepository(controller).set(AppLanguage.EN)
        assertEquals(emptyList<String>(), controller.applied)
    }
}
