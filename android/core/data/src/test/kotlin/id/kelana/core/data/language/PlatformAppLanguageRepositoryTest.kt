package id.kelana.core.data.language

import app.cash.turbine.test
import id.kelana.core.model.AppLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformAppLanguageRepositoryTest {
    private class FakeController(var tags: String = "") : AppLocaleController {
        val applied = mutableListOf<String>()
        override fun currentTags() = tags
        override fun apply(tags: String) {
            this.tags = tags
            applied += tags
        }
    }

    @Test
    fun startsFromPlatformValue() = runTest {
        PlatformAppLanguageRepository(FakeController("en-US")).language.test {
            assertEquals(AppLanguage.EN, awaitItem())
        }
        PlatformAppLanguageRepository(FakeController("")).language.test {
            assertEquals(AppLanguage.SYSTEM, awaitItem())
        }
        PlatformAppLanguageRepository(FakeController("fr")).language.test {
            assertEquals(AppLanguage.SYSTEM, awaitItem())
        }
    }

    @Test
    fun setAppliesTagAndEmits() = runTest {
        val controller = FakeController()
        val repo = PlatformAppLanguageRepository(controller)
        repo.language.test {
            assertEquals(AppLanguage.SYSTEM, awaitItem())
            repo.set(AppLanguage.ID)
            assertEquals(AppLanguage.ID, awaitItem())
            repo.set(AppLanguage.SYSTEM)
            assertEquals(AppLanguage.SYSTEM, awaitItem())
        }
        assertEquals(listOf("id", ""), controller.applied)
    }

    @Test
    fun settingSameLanguageDoesNotReapply() {
        val controller = FakeController("en")
        PlatformAppLanguageRepository(controller).set(AppLanguage.EN)
        assertEquals(emptyList<String>(), controller.applied)
    }
}
