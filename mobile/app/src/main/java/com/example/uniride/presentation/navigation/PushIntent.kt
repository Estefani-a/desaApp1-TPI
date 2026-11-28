package com.example.uniride.presentation.navigation

import android.content.Context
import android.content.Intent
import com.example.uniride.MainActivity
import com.example.uniride.data.push.PushTapIntentFactory
import com.example.uniride.domain.model.NotificationType
import com.example.uniride.domain.model.TripId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Keys of the intent extras of a tapped push: the same names as in the FCM data (docs/API.md §9). */
object PushExtras {
    const val TYPE = "type"
    const val TRIP_ID = "tripId"
}

/** [PushTapIntentFactory] backed by [openFromPushIntent], bound in `PushModule`. */
@Singleton
class ActivityPushTapIntentFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PushTapIntentFactory {
    override fun create(type: NotificationType, tripId: TripId): Intent =
        context.openFromPushIntent(type, tripId)
}

/**
 * Intent that opens the app on the screen of a push. `UniRideMessagingService` wraps it in the
 * `PendingIntent` of the notification. With the app open it reaches `MainActivity.onNewIntent`
 * instead of starting a second activity.
 */
fun Context.openFromPushIntent(type: NotificationType, tripId: TripId): Intent =
    Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(PushExtras.TYPE, type.name)
        .putExtra(PushExtras.TRIP_ID, tripId.value)

/** The screen this intent asks to open, or `null` if it does not come from a push. */
fun Intent.pushDestination(): PushDestination? =
    PushDestination.fromExtras(
        type = getStringExtra(PushExtras.TYPE),
        tripId = getStringExtra(PushExtras.TRIP_ID),
    )
