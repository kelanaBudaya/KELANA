package id.kelana.core.data.repository

import id.kelana.core.model.AppNotification
import id.kelana.core.model.NotificationEvent
import id.kelana.core.model.NotificationRepository
import id.kelana.core.model.ReminderChoice
import id.kelana.core.model.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Test double of [NotificationRepository] for other modules' unit tests. NOT bound in Hilt: the real
 * implementation lives in `:feature:notifikasi`. It only records what producers posted.
 */
class FakeNotificationRepository : NotificationRepository {
    private val _posted = MutableStateFlow<List<NotificationEvent>>(emptyList())

    /** Events passed to [post], oldest first. */
    val posted: List<NotificationEvent> get() = _posted.value

    var markAllReadCalls: Int = 0
        private set

    override fun notifications(): Flow<List<AppNotification>> = MutableStateFlow(emptyList())

    override fun unreadCount(): Flow<Int> = _posted.map { 0 }

    override suspend fun post(event: NotificationEvent) {
        _posted.value = _posted.value + event
    }

    override suspend fun markAllRead() {
        markAllReadCalls++
    }
}

/** Test double of [ReminderScheduler]; records calls. NOT bound in Hilt. */
class FakeReminderScheduler : ReminderScheduler {
    private val scheduled = mutableListOf<Pair<String, ReminderChoice>>()
    private val cancelled = mutableListOf<String>()

    /** Every `scheduleShowReminder` call, oldest first. */
    val scheduleCalls: List<Pair<String, ReminderChoice>> get() = scheduled.toList()

    /** Every `cancelShowReminder` call, oldest first. */
    val cancelCalls: List<String> get() = cancelled.toList()

    override suspend fun scheduleShowReminder(eventId: String, choice: ReminderChoice) {
        scheduled += eventId to choice
    }

    override suspend fun cancelShowReminder(eventId: String) {
        cancelled += eventId
    }
}
