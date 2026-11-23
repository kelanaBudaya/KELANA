package id.kelana.feature.notifikasi.ui

import app.cash.turbine.test
import id.kelana.core.model.AppNotification
import id.kelana.core.model.NotificationCategory
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.NotificationRepository
import id.kelana.feature.notifikasi.FakeSettings
import id.kelana.feature.notifikasi.FakeStorage
import id.kelana.feature.notifikasi.Jakarta
import id.kelana.feature.notifikasi.MainDispatcherRule
import id.kelana.feature.notifikasi.Now
import id.kelana.feature.notifikasi.domain.NotificationSettings
import id.kelana.feature.notifikasi.entry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** A repository over [FakeStorage] with the same read/markAllRead behaviour as the real one. */
private class StorageBackedRepository(val storage: FakeStorage) : NotificationRepository {
    var markAllReadCalls = 0
    override fun notifications(): Flow<List<AppNotification>> = storage.entries
    override fun unreadCount(): Flow<Int> = storage.entries.map { l -> l.count { !it.isRead } }
    override suspend fun post(event: NotificationEvent) = Unit
    override suspend fun markAllRead() {
        markAllReadCalls++
        storage.update { l -> l.map { it.copy(isRead = true) } }
    }
}

class NotifikasiViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    // Friday 20 November 2026, 10.40 in Jakarta.
    private val clock = Clock.fixed(Now, Jakarta)
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDate.of(2026, 11, day).atTime(hour, minute).atZone(Jakarta).toInstant()

    private fun viewModel(repo: NotificationRepository) = NotifikasiViewModel(repo, clock) { Jakarta }

    private fun sample() = FakeStorage(
        listOf(
            entry("today-rencana", at(20, 8), NotificationCategory.Rencana),
            entry("today-paspor", at(20, 9, 30), NotificationCategory.Paspor, placeId = "sonobudoyo"),
            entry("yesterday", at(19, 18), NotificationCategory.Rencana, eventId = "roro"),
            entry("older", at(14, 7), NotificationCategory.Info),
        ),
    )

    @Test
    fun groupsByDayNewestFirstAndLabelsTodayYesterdayAndOlderDates() = runTest {
        val vm = viewModel(StorageBackedRepository(sample()))
        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertEquals(
            listOf(
                Triple(LocalDate.of(2026, 11, 20), RelativeDay.Today, listOf("today-paspor", "today-rencana")),
                Triple(LocalDate.of(2026, 11, 19), RelativeDay.Yesterday, listOf("yesterday")),
                Triple(LocalDate.of(2026, 11, 14), RelativeDay.Other, listOf("older")),
            ),
            s.groups.map { g -> Triple(g.date, g.relative, g.items.map { it.id }) },
        )
    }

    @Test
    fun itemsCarryTheRealTimeOfTheEntryAndWhereATapLeads() = runTest {
        val s = viewModel(StorageBackedRepository(sample())).uiState.value
        val paspor = s.groups[0].items[0]
        assertEquals(LocalTime.of(9, 30), paspor.time)
        assertEquals("sonobudoyo", paspor.placeId)
        assertNull(paspor.eventId)
        assertEquals("roro", s.groups[1].items[0].eventId)
    }

    @Test
    fun chipsFilterByCategoryAndAllShowsEverythingIncludingInfo() = runTest {
        val vm = viewModel(StorageBackedRepository(sample()))
        fun ids() = vm.uiState.value.groups.flatMap { g -> g.items.map { it.id } }

        assertEquals(4, ids().size)
        vm.selectFilter(NotificationFilter.Rencana)
        assertEquals(NotificationFilter.Rencana, vm.uiState.value.filter)
        assertEquals(listOf("today-rencana", "yesterday"), ids())
        vm.selectFilter(NotificationFilter.Paspor)
        assertEquals(listOf("today-paspor"), ids())
        vm.selectFilter(NotificationFilter.Cerita)
        assertTrue(vm.uiState.value.groups.isEmpty())
        assertTrue(vm.uiState.value.isEmpty)
        vm.selectFilter(NotificationFilter.All)
        assertEquals(4, ids().size)
    }

    @Test
    fun noEntriesIsTheEmptyState() = runTest {
        val s = viewModel(StorageBackedRepository(FakeStorage())).uiState.value
        assertTrue(s.isEmpty)
        assertFalse(s.isLoading)
    }

    @Test
    fun openingMarksEverythingReadButTheDotStaysOnWhatWasUnread() = runTest {
        val repo = StorageBackedRepository(
            FakeStorage(
                listOf(
                    entry("new", at(20, 9), read = false),
                    entry("seen", at(20, 8), read = true),
                ),
            ),
        )
        val vm = viewModel(repo)
        assertEquals(1, repo.markAllReadCalls)
        assertTrue(repo.storage.state.value.all { it.isRead })
        val items = vm.uiState.value.groups.single().items.associateBy { it.id }
        assertTrue(items.getValue("new").isUnread)
        assertFalse(items.getValue("seen").isUnread)
    }

    @Test
    fun anEntryThatArrivesWhileOpenIsMarkedReadAndShowsItsDot() = runTest {
        val repo = StorageBackedRepository(FakeStorage())
        val vm = viewModel(repo)
        vm.uiState.test {
            assertTrue(awaitItem().isEmpty)
            repo.storage.update { listOf(entry("late", at(20, 10))) }
            var state = awaitItem()
            while (state.groups.isEmpty() || !state.groups.single().items.single().isUnread) state = awaitItem()
            assertTrue(state.groups.single().items.single().isUnread)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(repo.storage.state.value.single().isRead)
    }

    @Test
    fun aDayBoundaryFollowsTheZoneOfTheDevice() = runTest {
        // 17.30 UTC on the 19th is 00.30 on the 20th in Jakarta: Today there.
        val repo = StorageBackedRepository(FakeStorage(listOf(entry("midnight", Now.minus(Duration.ofHours(10)).minusSeconds(600)))))
        val s = viewModel(repo).uiState.value
        assertEquals(RelativeDay.Today, s.groups.single().relative)
    }
}

class PengaturanNotifViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun startsWithEverythingOn() = runTest {
        val vm = PengaturanNotifViewModel(FakeSettings())
        val s = vm.uiState.value
        assertTrue(s.isLoaded)
        assertEquals(NotificationSettings(showReminders = true, stampConfirmations = true, quietHours = true), s.settings)
    }

    @Test
    fun eachSwitchIsSavedAtOnceAndLeavesTheOthersAlone() = runTest {
        val settings = FakeSettings()
        val vm = PengaturanNotifViewModel(settings)

        vm.setShowReminders(false)
        assertEquals(NotificationSettings(showReminders = false), settings.state.value)
        assertFalse(vm.uiState.value.settings.showReminders)

        vm.setStampConfirmations(false)
        assertEquals(NotificationSettings(showReminders = false, stampConfirmations = false), settings.state.value)

        vm.setQuietHours(false)
        assertEquals(NotificationSettings(false, false, false), settings.state.value)

        vm.setShowReminders(true)
        assertEquals(NotificationSettings(true, false, false), vm.uiState.value.settings)
    }
}
