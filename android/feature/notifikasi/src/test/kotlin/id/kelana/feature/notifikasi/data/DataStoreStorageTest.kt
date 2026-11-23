package id.kelana.feature.notifikasi.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import id.kelana.core.model.NotificationCategory
import id.kelana.feature.notifikasi.Now
import id.kelana.feature.notifikasi.domain.NotificationSettings
import id.kelana.feature.notifikasi.entry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The real DataStore on a temp file: what is written is still there for a second instance (an app restart). */
class DataStoreStorageTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    private fun open(file: File): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    @Test
    fun entriesSurviveARestart() = runBlocking {
        val file = File(folder.root, "prefs.preferences_pb")
        val first = DataStoreNotificationStorage(open(file))
        first.update { listOf(entry("a", category = NotificationCategory.Paspor, placeId = "p1"), entry("b", read = true, eventId = "e1")) }
        scopes.first().cancel() // "kill the app"

        val second = DataStoreNotificationStorage(open(file))
        val loaded = second.entries.first()
        assertEquals(listOf("a", "b"), loaded.map { it.id })
        assertEquals(NotificationCategory.Paspor, loaded[0].category)
        assertEquals("p1", loaded[0].placeId)
        assertEquals(Now, loaded[0].createdAt)
        assertTrue(loaded[1].isRead)
        assertEquals("e1", loaded[1].eventId)
    }

    @Test
    fun anUnreadableValueReadsAsEmptyInsteadOfCrashing() = runBlocking {
        val store = open(File(folder.root, "bad.preferences_pb"))
        store.edit { it[stringPreferencesKey("notification_center_json")] = "{ not json" }
        assertTrue(DataStoreNotificationStorage(store).entries.first().isEmpty())
    }

    @Test
    fun settingsDefaultToAllOnAndPersist() = runBlocking {
        val file = File(folder.root, "settings.preferences_pb")
        val first = DataStoreNotificationSettings(open(file))
        assertEquals(NotificationSettings(), first.settings.first())
        first.update { it.copy(showReminders = false, quietHours = false) }
        scopes.first().cancel()

        val second = DataStoreNotificationSettings(open(file))
        assertEquals(NotificationSettings(showReminders = false, stampConfirmations = true, quietHours = false), second.settings.first())
    }
}
