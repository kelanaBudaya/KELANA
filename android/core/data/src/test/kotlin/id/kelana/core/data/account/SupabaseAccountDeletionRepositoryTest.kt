package id.kelana.core.data.account

import id.kelana.core.data.stamp.FakeOwnStampDao
import id.kelana.core.data.stamp.FakePendingDao
import id.kelana.core.data.stamp.FakeScheduler
import id.kelana.core.data.stamp.FakeSession
import id.kelana.core.data.stamp.signedIn
import id.kelana.core.database.entity.OwnStampEntity
import id.kelana.core.database.entity.PendingStampEntity
import id.kelana.core.model.AccountDeletionResult
import id.kelana.core.model.AccountDeletionSummary
import id.kelana.core.model.PlanCache
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.SessionState
import id.kelana.core.model.UserDataCleaner
import id.kelana.core.model.TripPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SupabaseAccountDeletionRepositoryTest {

    private class FakeAccountRemote : AccountRemote {
        override var isConfigured: Boolean = true
        var outcome = DeleteAccountOutcome.Deleted
        var counts = mutableMapOf<OwnTable, Int?>(OwnTable.Stamps to 3, OwnTable.Postcards to 2, OwnTable.Videos to 1)
        val countedFor = mutableListOf<String>()
        var deleteCalls = 0
        var sessionCleared = 0
        var onClearSession: () -> Unit = {}

        override suspend fun countOwn(table: OwnTable, userId: String): Int? {
            countedFor += userId
            return counts[table]
        }

        override suspend fun deleteAccount(): DeleteAccountOutcome {
            deleteCalls++
            return outcome
        }

        override suspend fun clearLocalSession() {
            sessionCleared++
            onClearSession()
        }
    }

    private class FakePlanCache : PlanCache {
        val tripState = MutableStateFlow<TripPlan?>(null)
        var cleared = 0
        override val trip: Flow<TripPlan?> = tripState
        override suspend fun save(trip: TripPlan) { tripState.value = trip }
        override suspend fun clear() { cleared++ }
        override val reminders: Flow<Map<String, ReminderChoice>> = MutableStateFlow(emptyMap())
        override suspend fun saveReminder(eventId: String, choice: ReminderChoice) = Unit
    }

    private val session = FakeSession(signedIn("user-1"))
    private val remote = FakeAccountRemote()
    private val pendingDao = FakePendingDao()
    private val ownStampDao = FakeOwnStampDao()
    private val scheduler = FakeScheduler()
    private val planCache = FakePlanCache()

    /** Records each run and whether the session was already cleared at that moment. */
    private inner class RecordingCleaner(private val fail: Boolean = false) : UserDataCleaner {
        val sessionClearedAtRun = mutableListOf<Int>()
        override suspend fun clear() {
            sessionClearedAtRun += remote.sessionCleared
            if (fail) throw IllegalStateException("cleaner")
        }
    }

    private val failingCleaner = RecordingCleaner(fail = true)
    private val cleaner = RecordingCleaner()

    private val repo = SupabaseAccountDeletionRepository(
        session, remote, pendingDao, ownStampDao, scheduler, planCache, linkedSetOf(failingCleaner, cleaner),
    )

    private fun seedLocal() {
        pendingDao.row.value = PendingStampEntity(
            userId = "user-1", placeId = "p1", lat = 0.0, lng = 0.0, accuracyM = 10.0, isMock = false,
            capturedAtMillis = 0L,
        )
        ownStampDao.rows.value = listOf(
            OwnStampEntity(userId = "user-1", placeId = "p1", stampedAtMillis = 1L),
            OwnStampEntity(userId = "other", placeId = "p1", stampedAtMillis = 1L),
        )
    }

    @Test
    fun summary_countsOwnRowsOfTheSignedInUser() = runTest {
        assertEquals(AccountDeletionSummary(3, 2, 1), repo.summary())
        assertEquals(listOf("user-1", "user-1", "user-1"), remote.countedFor)
    }

    @Test
    fun summary_unreadableCountStaysNull() = runTest {
        remote.counts[OwnTable.Videos] = null
        assertEquals(AccountDeletionSummary(3, 2, null), repo.summary())
    }

    @Test
    fun summary_guestOrNotConfigured_hasNoNumbers() = runTest {
        remote.isConfigured = false
        assertEquals(AccountDeletionSummary(), repo.summary())
        session.state.value = SessionState.Guest
        remote.isConfigured = true
        assertEquals(AccountDeletionSummary(), repo.summary())
        assertEquals(emptyList<String>(), remote.countedFor)
    }

    @Test
    fun deleted_clearsPendingPassportPlanAndThenSession() = runTest {
        seedLocal()
        // The session must be cleared last: by then the per-user data is already gone.
        remote.onClearSession = {
            assertNull(pendingDao.row.value)
            assertEquals(listOf("other"), ownStampDao.rows.value.map { it.userId })
            assertEquals(1, planCache.cleared)
            session.state.value = SessionState.Guest
        }
        assertEquals(AccountDeletionResult.Deleted, repo.deleteAccount())
        assertEquals(1, remote.deleteCalls)
        assertEquals(1, scheduler.cancelled)
        assertEquals(1, remote.sessionCleared)
        assertEquals(SessionState.Guest, session.state.value)
    }

    @Test
    fun deleted_runsEveryCleanerBeforeTheSessionIsCleared_evenIfOneFails() = runTest {
        assertEquals(AccountDeletionResult.Deleted, repo.deleteAccount())
        assertEquals(listOf(0), failingCleaner.sessionClearedAtRun)
        assertEquals(listOf(0), cleaner.sessionClearedAtRun)
        assertEquals(1, remote.sessionCleared)
    }

    @Test
    fun notDeleted_runsNoCleaner() = runTest {
        for (outcome in listOf(DeleteAccountOutcome.Refused, DeleteAccountOutcome.Unreachable)) {
            remote.outcome = outcome
            repo.deleteAccount()
        }
        assertEquals(emptyList<Int>(), cleaner.sessionClearedAtRun)
    }

    @Test
    fun failureOrNoAnswer_touchesNothingLocal() = runTest {
        for ((outcome, expected) in listOf(
            DeleteAccountOutcome.Refused to AccountDeletionResult.Failed,
            DeleteAccountOutcome.Unreachable to AccountDeletionResult.NetworkError,
        )) {
            seedLocal()
            remote.outcome = outcome
            assertEquals(expected, repo.deleteAccount())
            assertNotNull(pendingDao.row.value)
            assertEquals(2, ownStampDao.rows.value.size)
        }
        assertEquals(0, scheduler.cancelled)
        assertEquals(0, planCache.cleared)
        assertEquals(0, remote.sessionCleared)
        assertEquals(signedIn("user-1"), session.state.value)
    }

    @Test
    fun guestOrNotConfigured_neverCallsTheServer() = runTest {
        remote.isConfigured = false
        assertEquals(AccountDeletionResult.NotConfigured, repo.deleteAccount())
        session.state.value = SessionState.Guest
        remote.isConfigured = true
        assertEquals(AccountDeletionResult.NotSignedIn, repo.deleteAccount())
        assertEquals(0, remote.deleteCalls)
        assertEquals(0, remote.sessionCleared)
    }
}
