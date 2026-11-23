package id.kelana.feature.notifikasi.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import id.kelana.core.model.AppNotification
import id.kelana.core.model.NotificationCategory
import id.kelana.feature.notifikasi.domain.NotificationSettings
import id.kelana.feature.notifikasi.domain.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant

/** Where the notification center keeps its entries. Newest first. */
interface NotificationStorage {
    val entries: Flow<List<AppNotification>>

    /** Atomically replaces the stored list by `transform(current)`. */
    suspend fun update(transform: (List<AppNotification>) -> List<AppNotification>)
}

@Serializable
internal data class StoredNotification(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val isRead: Boolean,
    val placeId: String? = null,
    val eventId: String? = null,
)

private fun AppNotification.toStored() = StoredNotification(
    id = id,
    category = category.name,
    title = title,
    body = body,
    createdAtMillis = createdAt.toEpochMilli(),
    isRead = isRead,
    placeId = placeId,
    eventId = eventId,
)

private fun StoredNotification.toModel(): AppNotification? {
    val cat = NotificationCategory.entries.firstOrNull { it.name == category } ?: return null
    return AppNotification(id, cat, title, body, Instant.ofEpochMilli(createdAtMillis), isRead, placeId, eventId)
}

/**
 * Keeps the entries as one JSON string in the app's shared DataStore, so they survive a restart. An unreadable
 * value (an older format, a damaged file) reads as an empty list instead of crashing the screen.
 */
class DataStoreNotificationStorage(
    private val dataStore: DataStore<Preferences>,
) : NotificationStorage {

    override val entries: Flow<List<AppNotification>> = dataStore.data.map { decode(it[KEY]) }

    override suspend fun update(transform: (List<AppNotification>) -> List<AppNotification>) {
        dataStore.edit { prefs ->
            val next = transform(decode(prefs[KEY]))
            prefs[KEY] = JSON.encodeToString(LIST, next.map { it.toStored() })
        }
    }

    private fun decode(raw: String?): List<AppNotification> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            JSON.decodeFromString(LIST, raw).mapNotNull { it.toModel() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("notification_center_json")
        val JSON = Json { ignoreUnknownKeys = true }
        val LIST = ListSerializer(StoredNotification.serializer())
    }
}

/** Settings of Pengaturan notifikasi in the app's shared DataStore. */
class DataStoreNotificationSettings(
    private val dataStore: DataStore<Preferences>,
) : NotificationSettingsRepository {

    override val settings: Flow<NotificationSettings> = dataStore.data.map(::read)

    override suspend fun update(transform: (NotificationSettings) -> NotificationSettings) {
        dataStore.edit { prefs ->
            val next = transform(read(prefs))
            prefs[SHOW_REMINDERS] = next.showReminders
            prefs[STAMP_CONFIRMATIONS] = next.stampConfirmations
            prefs[QUIET_HOURS] = next.quietHours
        }
    }

    private fun read(prefs: Preferences): NotificationSettings {
        val defaults = NotificationSettings()
        return NotificationSettings(
            showReminders = prefs[SHOW_REMINDERS] ?: defaults.showReminders,
            stampConfirmations = prefs[STAMP_CONFIRMATIONS] ?: defaults.stampConfirmations,
            quietHours = prefs[QUIET_HOURS] ?: defaults.quietHours,
        )
    }

    private companion object {
        val SHOW_REMINDERS = booleanPreferencesKey("notif_setting_show_reminders")
        val STAMP_CONFIRMATIONS = booleanPreferencesKey("notif_setting_stamp_confirmations")
        val QUIET_HOURS = booleanPreferencesKey("notif_setting_quiet_hours")
    }
}
