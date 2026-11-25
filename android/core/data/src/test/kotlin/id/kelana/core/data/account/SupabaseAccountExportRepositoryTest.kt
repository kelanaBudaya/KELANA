package id.kelana.core.data.account

import id.kelana.core.data.stamp.FakeSession
import id.kelana.core.data.stamp.signedIn
import id.kelana.core.model.AccountExportResult
import id.kelana.core.model.SessionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SupabaseAccountExportRepositoryTest {

    private class FakeRemote : AccountExportRemote {
        override var isConfigured = true
        var outcome: ExportAccountOutcome = ExportAccountOutcome.Exported(doc("user-1"))
        var calls = 0
        override suspend fun exportAccount(): ExportAccountOutcome {
            calls++
            return outcome
        }
    }

    private class FakeWriter : ExportDocumentWriter {
        val written = mutableMapOf<String, String>()
        var fail = false
        override suspend fun write(destination: String, content: String) {
            if (fail) throw IOException("disk")
            written[destination] = content
        }
    }

    private val session = FakeSession(signedIn("user-1"))
    private val remote = FakeRemote()
    private val writer = FakeWriter()
    private val repo = SupabaseAccountExportRepository(session, remote, writer)

    @Test
    fun exportOfTheSignedInUser_isReady_unchanged() = runTest {
        assertEquals(AccountExportResult.Ready(doc("user-1")), repo.export())
    }

    @Test
    fun exportOfAnotherUser_orNotAnExport_fails() = runTest {
        for (body in listOf(doc("user-2"), "{}", "not json", "[]", doc("user-1").replace("\"version\": 1", "\"version\": 2"))) {
            remote.outcome = ExportAccountOutcome.Exported(body)
            assertEquals(body, AccountExportResult.Failed, repo.export())
        }
    }

    @Test
    fun outcomesMap() = runTest {
        remote.outcome = ExportAccountOutcome.Unreachable
        assertEquals(AccountExportResult.NetworkError, repo.export())
        remote.outcome = ExportAccountOutcome.Refused
        assertEquals(AccountExportResult.Failed, repo.export())
    }

    @Test
    fun guestOrNotConfigured_sendsNothing() = runTest {
        remote.isConfigured = false
        assertEquals(AccountExportResult.NotConfigured, repo.export())
        session.state.value = SessionState.Guest
        assertEquals(AccountExportResult.NotSignedIn, repo.export())
        assertEquals(0, remote.calls)
    }

    @Test
    fun save_writesTheDocument_orReportsFailure() = runTest {
        assertTrue(repo.save("content://doc/1", "{\"a\":1}"))
        assertEquals("{\"a\":1}", writer.written["content://doc/1"])
        writer.fail = true
        assertFalse(repo.save("content://doc/2", "{}"))
    }

    private companion object {
        fun doc(userId: String) =
            """{ "format": "kelana-account-export", "version": 1, "account": { "id": "$userId" }, "stamps": [] }"""
    }
}
