package id.kelana.feature.notifikasi.data

import app.cash.turbine.test
import id.kelana.core.model.NotificationCategory
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.ReminderTiming
import id.kelana.feature.notifikasi.FakeEvents
import id.kelana.feature.notifikasi.FakeForeground
import id.kelana.feature.notifikasi.FakeNotifier
import id.kelana.feature.notifikasi.FakePlaces
import id.kelana.feature.notifikasi.FakeSettings
import id.kelana.feature.notifikasi.FakeStorage
import id.kelana.feature.notifikasi.Jakarta
import id.kelana.feature.notifikasi.Now
import id.kelana.feature.notifikasi.R
import id.kelana.feature.notifikasi.RecordingText
import id.kelana.feature.notifikasi.domain.NotificationContentResolver
import id.kelana.feature.notifikasi.domain.NotificationSettings
import id.kelana.feature.notifikasi.entry
import id.kelana.feature.notifikasi.place
import id.kelana.feature.notifikasi.show
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private class MutableClock(var now: Instant) : Clock() {
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?): Clock = this
    override fun instant(): Instant = now
}

class LocalNotificationRepositoryTest {
    private val storage = FakeStorage()
    private val settings = FakeSettings()
    private val notifier = FakeNotifier()
    private val foreground = FakeForeground()
    private val clock = MutableClock(Now) // 10.40 in Jakarta
    private var nextId = 0

    private val repo = LocalNotificationRepository(
        storage = storage,
        settings = settings,
        resolver = NotificationContentResolver(
            FakePlaces(listOf(place())),
            FakeEvents(listOf(show(), show(id = "unchecked", checkedAt = null))),
            RecordingText,
        ),
        notifier = notifier,
        foreground = foreground,
        clock = clock,
        zone = { Jakarta },
        newId = { "n${nextId++}" },
    )

    // ---- posting each event

    @Test
    fun stampConfirmedBecomesAPasporEntryNamingTheCapAndThePlace() = runTest {
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        val e = repo.notifications().first().single()
        assertEquals(NotificationCategory.Paspor, e.category)
        assertEquals("${R.string.notif_stamp_title}|YK-02", e.title)
        assertEquals("${R.string.notif_stamp_body}|Museum Sonobudoyo", e.body)
        assertEquals("sonobudoyo", e.placeId)
        assertNull(e.eventId)
        assertFalse(e.isRead)
        assertEquals(Now, e.createdAt)
    }

    @Test
    fun stampForAnUnknownPlaceStillMakesAnEntry() = runTest {
        repo.post(NotificationEvent.StampConfirmed("gone"))
        val e = repo.notifications().first().single()
        assertEquals("${R.string.notif_stamp_title_unknown}|", e.title)
        assertEquals("gone", e.placeId)
    }

    @Test
    fun showReminderBecomesARencanaEntryForEachTiming() = runTest {
        repo.post(NotificationEvent.ShowReminderDue("roro", ReminderTiming.WhenToLeave))
        repo.post(NotificationEvent.ShowReminderDue("roro", ReminderTiming.OneHourBefore))
        repo.post(NotificationEvent.ShowReminderDue("roro", ReminderTiming.ThirtyMinutesBefore))
        val all = repo.notifications().first()
        assertEquals(3, all.size)
        assertTrue(all.all { it.category == NotificationCategory.Rencana && it.eventId == "roro" && it.placeId == "prambanan" })
        // Newest first: the last posted leads.
        assertEquals("${R.string.notif_reminder_title_half}|Sendratari Roro Jonggrang", all[0].title)
        assertEquals("${R.string.notif_reminder_title_hour}|Sendratari Roro Jonggrang", all[1].title)
        assertEquals("${R.string.notif_reminder_title_leave}|Prambanan", all[2].title)
        // The show time is written with its check date.
        assertEquals("${R.string.notif_reminder_body}|Sendratari Roro Jonggrang|19.30|Prambanan|20 Nov 2026", all[0].body)
    }

    @Test
    fun showTimeWithoutACheckDateIsNeverWritten() = runTest {
        repo.post(NotificationEvent.ShowReminderDue("unchecked", ReminderTiming.OneHourBefore))
        val e = repo.notifications().first().single()
        assertEquals("${R.string.notif_reminder_body_checking}|Sendratari Roro Jonggrang|Prambanan", e.body)
        assertFalse(e.body.contains("19.30"))
    }

    @Test
    fun reminderForAnUnknownEventStillMakesAnEntry() = runTest {
        repo.post(NotificationEvent.ShowReminderDue("gone", ReminderTiming.WhenToLeave))
        val e = repo.notifications().first().single()
        assertEquals("${R.string.notif_reminder_title_unknown}|", e.title)
        assertEquals("gone", e.eventId)
    }

    // ---- ordering, unread count, mark all read

    @Test
    fun entriesAreNewestFirst() = runTest {
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        clock.now = Now.plusSeconds(60)
        repo.post(NotificationEvent.ShowReminderDue("roro", ReminderTiming.WhenToLeave))
        clock.now = Now.plusSeconds(120)
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        assertEquals(listOf("n2", "n1", "n0"), repo.notifications().first().map { it.id })
    }

    @Test
    fun entriesPostedAtTheSameInstantStillLeadWithTheNewest() = runTest {
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        assertEquals(listOf("n1", "n0"), repo.notifications().first().map { it.id })
    }

    @Test
    fun unreadCountGrowsWithPostsAndMarkAllReadClearsIt() = runTest {
        repo.unreadCount().test {
            assertEquals(0, awaitItem())
            repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
            assertEquals(1, awaitItem())
            repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
            assertEquals(2, awaitItem())
            repo.markAllRead()
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repo.notifications().first().all { it.isRead })
    }

    @Test
    fun markAllReadWithNothingUnreadChangesNothing() = runTest {
        repo.markAllRead()
        assertTrue(repo.notifications().first().isEmpty())
    }

    // ---- the 100-entry cap

    @Test
    fun keepsAtMostOneHundredEntriesAndDropsTheOldest() = runTest {
        repeat(MAX_NOTIFICATIONS + 5) { i ->
            clock.now = Now.plusSeconds(i.toLong())
            repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        }
        val all = repo.notifications().first()
        assertEquals(100, all.size)
        assertEquals("n104", all.first().id)
        assertEquals("n5", all.last().id) // n0..n4 were dropped
    }

    // ---- when a system notification is shown

    @Test
    fun showsASystemNotificationWhenEverythingAllows() = runTest {
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        assertEquals(1, notifier.shown.size)
        assertEquals(repo.notifications().first().single(), notifier.shown.single())
    }

    @Test
    fun withoutPermissionTheEntryIsStoredButNothingIsShown() = runTest {
        notifier.allowed = false
        repo.post(NotificationEvent.ShowReminderDue("roro", ReminderTiming.WhenToLeave))
        assertTrue(notifier.shown.isEmpty())
        assertEquals(1, repo.notifications().first().size)
    }

    @Test
    fun aDisabledCategoryIsStoredButNotShown() = runTest {
        settings.state.value = NotificationSettings(showReminders = false)
        repo.post(NotificationEvent.ShowReminderDue("roro", ReminderTiming.WhenToLeave))
        assertTrue(notifier.shown.isEmpty())
        assertEquals(1, repo.notifications().first().size)
        // The other category still shows.
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        assertEquals(listOf(NotificationCategory.Paspor), notifier.shown.map { it.category })

        settings.state.value = NotificationSettings(stampConfirmations = false)
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun inForegroundTheEntryIsStoredButNotShown() = runTest {
        foreground.foreground = true
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        assertTrue(notifier.shown.isEmpty())
        assertEquals(1, repo.notifications().first().size)
    }

    // ---- quiet hours 21.00 to 06.00 (Jakarta in these tests)

    private suspend fun postAt(hour: Int, minute: Int) {
        clock.now = LocalDate.of(2026, 11, 20).atTime(hour, minute).atZone(Jakarta).toInstant()
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
    }

    @Test
    fun quietHoursHoldBackTheSystemNotificationButKeepTheEntry() = runTest {
        for ((h, m) in listOf(21 to 0, 23 to 59, 0 to 0, 5 to 59)) {
            notifier.shown.clear()
            postAt(h, m)
            assertTrue("$h:$m should be quiet", notifier.shown.isEmpty())
        }
        assertEquals(4, repo.notifications().first().size)
    }

    @Test
    fun outsideQuietHoursItIsShown() = runTest {
        for ((h, m) in listOf(6 to 0, 12 to 0, 20 to 59)) {
            notifier.shown.clear()
            postAt(h, m)
            assertEquals("$h:$m should show", 1, notifier.shown.size)
        }
    }

    @Test
    fun withQuietHoursSwitchedOffANightNotificationIsShown() = runTest {
        settings.state.value = NotificationSettings(quietHours = false)
        postAt(23, 0)
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun clockGapSanityBetweenTwoPostsUsesTheClockAtPostTime() = runTest {
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        clock.now = Now.plus(Duration.ofHours(1))
        repo.post(NotificationEvent.StampConfirmed("sonobudoyo"))
        val times = repo.notifications().first().map { it.createdAt }
        assertEquals(listOf(Now.plus(Duration.ofHours(1)), Now), times)
    }
}
