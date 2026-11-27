package com.example.uniride.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.uniride.R
import com.example.uniride.domain.model.NotificationType
import com.example.uniride.presentation.navigation.openFromPushIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Displays system notifications for incoming RF04 push messages. */
interface PushNotifier {
    fun ensureChannel()
    fun show(payload: PushPayload)
}

/**
 * Android notification renderer for RF04 pushes (docs/API.md §9).
 *
 * Uses [openFromPushIntent] so tapping the notification opens C4 (Panel del conductor) or B6
 * (Mi viaje) on top of the Home tab, respecting `POST_NOTIFICATIONS` on Android 13+ (RNF08).
 */
@Singleton
class AndroidPushNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PushNotifier {

    override fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.push_channel_reservations_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.push_channel_reservations_description)
        }
        manager.createNotificationChannel(channel)
    }

    override fun show(payload: PushPayload) {
        ensureChannel()
        if (!canPostNotifications()) return

        val title = payload.title ?: context.getString(defaultTitleRes(payload.type))
        val body = payload.body ?: context.getString(defaultBodyRes(payload.type))
        val tapIntent = context.openFromPushIntent(payload.type, payload.tripId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.notificationIntId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(payload.notificationIntId, notification)
        } catch (_: SecurityException) {
            // Permission revoked right before notify (RNF08): degrade silently.
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @StringRes
    private fun defaultTitleRes(type: NotificationType): Int = when (type) {
        NotificationType.NUEVA_SOLICITUD -> R.string.push_default_title_new_request
        NotificationType.SOLICITUD_ACEPTADA -> R.string.push_default_title_accepted
        NotificationType.SOLICITUD_RECHAZADA -> R.string.push_default_title_rejected
    }

    @StringRes
    private fun defaultBodyRes(type: NotificationType): Int = when (type) {
        NotificationType.NUEVA_SOLICITUD -> R.string.push_default_body_new_request
        NotificationType.SOLICITUD_ACEPTADA -> R.string.push_default_body_accepted
        NotificationType.SOLICITUD_RECHAZADA -> R.string.push_default_body_rejected
    }

    companion object {
        const val CHANNEL_ID = "uniride_reservations"
    }
}
