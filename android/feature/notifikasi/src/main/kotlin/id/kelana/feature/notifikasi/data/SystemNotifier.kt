package id.kelana.feature.notifikasi.data

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.kelana.core.model.AppNotification
import id.kelana.core.model.NotificationCategory
import id.kelana.feature.notifikasi.R

/** Shows a notification in the status bar. A seam so the repository can be tested without Android. */
interface SystemNotifier {
    /** True when the OS lets this app post: POST_NOTIFICATIONS granted (API 33+) and not blocked in system settings. */
    fun canShow(): Boolean

    fun show(notification: AppNotification)
}

/** Whether the app has a visible screen right now. In the foreground an entry is stored but not shown in the status bar. */
fun interface AppForeground {
    fun isForeground(): Boolean
}

/** Channel ids: Rencana, Paspor and Info (Cerita and Info entries share Info). */
internal object NotificationChannels {
    const val RENCANA = "kelana_rencana"
    const val PASPOR = "kelana_paspor"
    const val INFO = "kelana_info"

    fun idOf(category: NotificationCategory): String = when (category) {
        NotificationCategory.Rencana -> RENCANA
        NotificationCategory.Paspor -> PASPOR
        NotificationCategory.Cerita, NotificationCategory.Info -> INFO
    }
}

class AndroidSystemNotifier(private val context: Context) : SystemNotifier {

    override fun canShow(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    override fun show(notification: AppNotification) {
        ensureChannels()
        // Tapping opens the app. PROVISIONAL: it does not deep-link to the place or event yet (the entry in the
        // center does), because :app's activity has no deep link handling in this phase.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val notificationId = notification.id.hashCode()
        val pending = launch?.let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val built = NotificationCompat.Builder(context, NotificationChannels.idOf(notification.category))
            .setSmallIcon(R.drawable.notifikasi_ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, built)
        } catch (_: SecurityException) {
            // The permission was revoked between canShow() and notify(); the entry is still in the center.
        }
    }

    private fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.RENCANA,
                context.getString(R.string.notif_channel_rencana),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.PASPOR,
                context.getString(R.string.notif_channel_paspor),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.INFO,
                context.getString(R.string.notif_channel_info),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }
}

/** Foreground check without a lifecycle dependency: asks the OS for this process's importance. */
class ProcessForeground : AppForeground {
    override fun isForeground(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
}
