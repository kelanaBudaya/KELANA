package id.kelana.feature.notifikasi.data

import id.kelana.core.model.AppNotification
import id.kelana.core.model.NotificationCategory
import id.kelana.feature.notifikasi.FakeStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NotifikasiUserDataCleanerTest {

    private val entry = AppNotification(
        id = "n1",
        category = NotificationCategory.Paspor,
        title = "Cap tersimpan",
        body = "Keraton",
        createdAt = Instant.parse("2026-11-20T03:00:00Z"),
        isRead = false,
    )

    @Test
    fun clearsEntries_cancelsReminders_dismissesShown() = runTest {
        val storage = FakeStorage(listOf(entry))
        var cancelled = 0
        var dismissed = 0

        NotifikasiUserDataCleaner(storage, { cancelled++ }, { dismissed++ }).clear()

        assertTrue(storage.entries.first().isEmpty())
        assertEquals(1, cancelled)
        assertEquals(1, dismissed)
    }

    @Test
    fun aFailingStep_doesNotStopTheOthers_andIsReported() = runTest {
        val storage = FakeStorage(listOf(entry))
        var dismissed = 0

        val thrown = runCatching {
            NotifikasiUserDataCleaner(storage, { throw IllegalStateException("work") }, { dismissed++ }).clear()
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertEquals(1, dismissed)
        assertTrue(storage.entries.first().isEmpty())
    }
}
