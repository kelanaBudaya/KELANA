package id.kelana.core.data.plan

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Logic runs on an in-memory DataStore (the real file store cannot replace its file twice on Windows, so several
 * writes in one test fail there). One real-file test with a single write covers the restart.
 */
class DataStorePlanBackpackRepositoryTest {
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

    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.value = transform(state.value)
            return state.value
        }
    }

    private fun repo() = DataStorePlanBackpackRepository(MemoryStore())

    @Test
    fun startsEmpty() = runBlocking {
        assertEquals(emptyList<String>(), repo().placeIds().first())
    }

    @Test
    fun addKeepsOrderAndIgnoresDuplicates() = runBlocking {
        val r = repo()
        r.addPlace("b")
        r.addPlace("a")
        r.addPlace("b")
        assertEquals(listOf("b", "a"), r.placeIds().first())
    }

    @Test
    fun removeDropsOnlyThatPlace() = runBlocking {
        val r = repo()
        r.addPlace("a")
        r.addPlace("b")
        r.removePlace("a")
        r.removePlace("zzz")
        assertEquals(listOf("b"), r.placeIds().first())
    }

    @Test
    fun blankIdIsIgnored() = runBlocking {
        val r = repo()
        r.addPlace(" ")
        assertEquals(emptyList<String>(), r.placeIds().first())
    }

    @Test
    fun survivesARestart() = runBlocking {
        val file = File(folder.root, "p.preferences_pb")
        val first = DataStorePlanBackpackRepository(open(file))
        first.addPlace("a-b")
        // Wait for the first store to release the file; DataStore refuses two active stores on one file.
        scopes.first().coroutineContext[Job]!!.cancelAndJoin()
        assertEquals(listOf("a-b"), DataStorePlanBackpackRepository(open(file)).placeIds().first())
    }
}
