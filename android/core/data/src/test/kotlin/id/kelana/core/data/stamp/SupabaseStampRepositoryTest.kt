package id.kelana.core.data.stamp

import app.cash.turbine.test
import id.kelana.core.data.repository.FakeNotificationRepository
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.SessionState
import id.kelana.core.model.Stamp
import id.kelana.core.model.StampRejectReason
import id.kelana.core.model.StampRequest
import id.kelana.core.model.StampResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SupabaseStampRepositoryTest {
    private val placeId = "123e4567-e89b-42d3-a456-426614174000"
    private val now = Instant.parse("2026-11-21T10:30:00.000Z")

    private val clock = MutableClock(now)
    private val pendingDao = FakePendingDao()
    private val ownDao = FakeOwnStampDao()
    private val remote = FakeStampRemote()
    private val integrity = FakeIntegrity()
    private val session = FakeSession(signedIn())
    private val scheduler = FakeScheduler()
    private val network = FakeNetwork(online = true)
    private val notifications = FakeNotificationRepository()

    private val cache = OwnStampsCache(remote, ownDao, clock)
    private val sender = StampSender(pendingDao, cache, remote, integrity, session, clock, notifications)
    private val repo = SupabaseStampRepository(
        session, remote, pendingDao, ownDao, cache, sender, scheduler, network, clock,
    )

    private fun request(capturedAt: Instant = now.minusSeconds(5)) =
        StampRequest(placeId, -7.8053, 110.3642, 12.34, false, capturedAt)

    // ---- guests and configuration

    @Test
    fun guest_needsLogin_andNothingIsStoredOrSent() = runTest {
        session.state.value = SessionState.Guest
        assertEquals(StampResult.NeedsLogin, repo.requestStamp(request()))
        assertNull(pendingDao.row.value)
        assertTrue(remote.sent.isEmpty())
        assertTrue(integrity.nonces.isEmpty())
    }

    @Test
    fun guest_seesNoStamps() = runTest {
        session.state.value = SessionState.Guest
        ownDao.rows.value = listOf(id.kelana.core.database.entity.OwnStampEntity("user-1", placeId, 1L))
        repo.stamps().test {
            assertEquals(emptyList<Stamp>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notConfigured_isReportedAndNothingIsStored() = runTest {
        remote.isConfigured = false
        assertEquals(StampResult.NotConfigured, repo.requestStamp(request()))
        assertNull(pendingDao.row.value)
        assertTrue(remote.sent.isEmpty())
    }

    @Test
    fun integrityNotConfigured_dropsTheFixAndReportsNotConfigured() = runTest {
        integrity.result = IntegrityTokenResult.NotConfigured
        assertEquals(StampResult.NotConfigured, repo.requestStamp(request()))
        assertNull(pendingDao.row.value)
        assertTrue(remote.sent.isEmpty())
    }

    @Test
    fun unusableFix_isRejectedLocallyAndNeverStored() = runTest {
        val bad = StampRequest(placeId, Double.NaN, 110.0, 10.0, false, now)
        assertEquals(StampResult.Rejected(StampRejectReason.BadRequest), repo.requestStamp(bad))
        assertNull(pendingDao.row.value)
    }

    // ---- 30-minute expiry

    @Test
    fun exactlyThirtyMinutesOld_isStillSent() = runTest {
        val result = repo.requestStamp(request(capturedAt = now.minus(Duration.ofMinutes(30))))
        assertEquals(StampResult.Stamped, result)
        assertEquals(1, remote.sent.size)
    }

    @Test
    fun olderThanThirtyMinutes_isDroppedOnTheClientAsExpired() = runTest {
        val result = repo.requestStamp(request(capturedAt = now.minus(Duration.ofMinutes(30)).minusMillis(1)))
        assertEquals(StampResult.Expired, result)
        assertNull(pendingDao.row.value)
        assertTrue(remote.sent.isEmpty())
    }

    @Test
    fun queuedRequestThatAgesPastThirtyMinutes_isDroppedAtSendTime() = runTest {
        network.online = false
        assertEquals(StampResult.Queued, repo.requestStamp(request()))
        assertNotNull(pendingDao.row.value)

        clock.now = now.plus(Duration.ofMinutes(31))
        assertEquals(StampResult.Expired, sender.sendPending())
        assertNull(pendingDao.row.value)
        assertTrue(remote.sent.isEmpty())
        assertTrue(integrity.nonces.isEmpty())
    }

    @Test
    fun queuedRequestAtExactlyThirtyMinutes_isStillSent() = runTest {
        network.online = false
        val captured = now.minusSeconds(5)
        repo.requestStamp(request(captured))
        clock.now = captured.plus(Duration.ofMinutes(30))
        assertEquals(StampResult.Stamped, sender.sendPending())
    }

    // ---- the pending row is deleted on ANY server answer

    @Test
    fun stampedAnswer_deletesTheRow() = runTest {
        remote.outcome = VerifyOutcome.Answer("stamped", null, null)
        assertEquals(StampResult.Stamped, repo.requestStamp(request()))
        assertNull(pendingDao.row.value)
    }

    @Test
    fun alreadyStampedAnswer_deletesTheRow() = runTest {
        remote.outcome = VerifyOutcome.Answer("already_stamped", null, null)
        assertEquals(StampResult.AlreadyStamped, repo.requestStamp(request()))
        assertNull(pendingDao.row.value)
    }

    @Test
    fun everyRejectionReason_deletesTheRow_andIsMappedExactly() = runTest {
        for (reason in StampRejectReason.entries) {
            val distance = if (reason == StampRejectReason.TooFar) 420 else null
            remote.outcome = VerifyOutcome.Answer("rejected", reason.wire, distance)
            val result = repo.requestStamp(request())
            assertEquals(StampResult.Rejected(reason, distance), result)
            assertNull("row must be gone after ${reason.wire}", pendingDao.row.value)
        }
    }

    @Test
    fun unknownReason_becomesBadRequest_andDeletesTheRow() = runTest {
        remote.outcome = VerifyOutcome.Answer("rejected", "something_new", 5)
        assertEquals(StampResult.Rejected(StampRejectReason.BadRequest, null), repo.requestStamp(request()))
        assertNull(pendingDao.row.value)
    }

    @Test
    fun noAnswer_keepsTheRow_schedulesRetry_andReportsNetworkError() = runTest {
        remote.outcome = VerifyOutcome.Unreachable
        assertEquals(StampResult.NetworkError, repo.requestStamp(request()))
        assertNotNull(pendingDao.row.value)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun integrityUnavailable_keepsTheRow_andDoesNotCallTheServer() = runTest {
        integrity.result = IntegrityTokenResult.Unavailable
        assertEquals(StampResult.NetworkError, repo.requestStamp(request()))
        assertNotNull(pendingDao.row.value)
        assertTrue(remote.sent.isEmpty())
    }

    // ---- offline queue

    @Test
    fun offline_queuesOneRequest_schedulesWork_andShowsPending() = runTest {
        network.online = false
        assertEquals(StampResult.Queued, repo.requestStamp(request(capturedAt = now.minusSeconds(60))))
        assertEquals(1, scheduler.scheduled)
        assertTrue(remote.sent.isEmpty())
        // The integrity token is requested at SEND time, not when queueing.
        assertTrue(integrity.nonces.isEmpty())
        repo.pending().test {
            val pending = awaitItem()!!
            assertEquals(placeId, pending.placeId)
            assertEquals(now.minusSeconds(60), pending.capturedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aSecondRequestReplacesTheFirst_onlyOneIsQueued() = runTest {
        network.online = false
        repo.requestStamp(request())
        val other = "00000000-0000-4000-8000-000000000001"
        repo.requestStamp(StampRequest(other, 0.0, 0.0, 5.0, false, now))
        assertEquals(other, pendingDao.row.value!!.placeId)
    }

    @Test
    fun cancelPending_deletesTheRowAndCancelsTheJob() = runTest {
        network.online = false
        repo.requestStamp(request())
        repo.cancelPending()
        assertNull(pendingDao.row.value)
        assertEquals(1, scheduler.cancelled)
        repo.pending().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun workerPath_sendsTheQueuedRequestOnceBackOnline() = runTest {
        network.online = false
        repo.requestStamp(request())
        // What StampSendWorker does when the network returns:
        assertEquals(StampResult.Stamped, sender.sendPending())
        assertNull(pendingDao.row.value)
        assertEquals(null, sender.sendPending()) // nothing pending any more
    }

    @Test
    fun queuedRequestOfAnotherAccount_isDroppedNotSent() = runTest {
        network.online = false
        repo.requestStamp(request())
        session.state.value = signedIn("user-2")
        assertEquals(StampResult.NeedsLogin, sender.sendPending())
        assertNull(pendingDao.row.value)
        assertTrue(remote.sent.isEmpty())
    }

    // ---- send time token and request body

    @Test
    fun tokenIsRequestedAtSendTime_withTheNonceOfTheExactBody() = runTest {
        repo.requestStamp(request())
        val body = remote.sent.single()
        val expectedNonce = stampNonce(body.placeId, body.lat, body.lng, body.accuracyM, body.capturedAt)
        assertEquals(listOf(expectedNonce), integrity.nonces)
        assertEquals("fake-token", body.integrityToken)
    }

    @Test
    fun requestBody_isRoundedAndFormattedExactly() = runTest {
        val captured = Instant.parse("2026-11-21T10:29:55.123456Z")
        repo.requestStamp(StampRequest(placeId.uppercase(), -7.80530049, 110.36419951, 12.34, false, captured))
        val body = remote.sent.single()
        assertEquals(placeId, body.placeId)
        assertEquals(-7.805300, body.lat, 0.0)
        assertEquals(110.3642, body.lng, 0.0)
        assertEquals(12.3, body.accuracyM, 0.0)
        assertEquals("2026-11-21T10:29:55.123Z", body.capturedAt)
        assertFalse(body.isMock)
    }

    // ---- stamps() / passport

    @Test
    fun stampDoesNotAppearBeforeTheServerSaysStamped() = runTest {
        remote.fetchFails = true // the server cannot be read either, so only a confirmed stamp can add one
        network.online = false
        repo.stamps().test {
            assertEquals(emptyList<Stamp>(), awaitItem())
            repo.requestStamp(request())
            expectNoEvents()
            assertEquals(emptyList<Stamp>(), ownDao.rows.value)

            // Rejected: still nothing.
            network.online = true
            remote.outcome = VerifyOutcome.Answer("rejected", "too_far", 900)
            sender.sendPending()
            expectNoEvents()

            // Confirmed by the server: now it appears.
            remote.outcome = VerifyOutcome.Answer("stamped", null, null)
            repo.requestStamp(request())
            assertEquals(listOf(placeId), awaitItem().map { it.placeId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmedStamp_comesFromTheServerList_whenReachable() = runTest {
        remote.serverStamps = listOf(StampDto(placeId, "2026-11-21T03:30:00.123456+00:00"))
        repo.requestStamp(request())
        val stamps = repo.stamps().first { it.isNotEmpty() }
        assertEquals(Stamp(placeId, Instant.parse("2026-11-21T03:30:00.123Z")), stamps.single()) // cache keeps epoch millis
    }

    @Test
    fun passportCache_isServedOfflineAndPerAccount() = runTest {
        remote.fetchFails = true
        ownDao.rows.value = listOf(
            id.kelana.core.database.entity.OwnStampEntity("user-1", placeId, 1_000L),
            id.kelana.core.database.entity.OwnStampEntity("user-2", "other", 2_000L),
        )
        val stamps = repo.stamps().first { it.isNotEmpty() }
        assertEquals(listOf(placeId), stamps.map { it.placeId })
    }

    // ---- notification center: one event, only for a confirmed stamp

    @Test
    fun stampedAnswer_postsStampConfirmedOnce() = runTest {
        remote.outcome = VerifyOutcome.Answer("stamped", null, null)
        assertEquals(StampResult.Stamped, repo.requestStamp(request()))
        assertEquals(listOf<NotificationEvent>(NotificationEvent.StampConfirmed(placeId)), notifications.posted)
    }

    @Test
    fun queuedStampSentLater_postsStampConfirmedWhenTheServerConfirms() = runTest {
        network.online = false
        repo.requestStamp(request())
        assertTrue(notifications.posted.isEmpty())
        assertEquals(StampResult.Stamped, sender.sendPending())
        assertEquals(listOf<NotificationEvent>(NotificationEvent.StampConfirmed(placeId)), notifications.posted)
    }

    @Test
    fun alreadyStamped_rejected_expired_andNetworkError_postNothing() = runTest {
        remote.outcome = VerifyOutcome.Answer("already_stamped", null, null)
        assertEquals(StampResult.AlreadyStamped, repo.requestStamp(request()))
        for (reason in StampRejectReason.entries) {
            remote.outcome = VerifyOutcome.Answer("rejected", reason.wire, null)
            repo.requestStamp(request())
        }
        assertEquals(StampResult.Expired, repo.requestStamp(request(capturedAt = now.minus(Duration.ofMinutes(31)))))
        remote.outcome = VerifyOutcome.Unreachable
        repo.requestStamp(request())
        assertTrue(notifications.posted.isEmpty())
    }
}
