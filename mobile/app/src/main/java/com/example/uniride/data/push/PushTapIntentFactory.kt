package com.example.uniride.data.push

import android.content.Intent
import com.example.uniride.domain.model.NotificationType
import com.example.uniride.domain.model.TripId

/**
 * Builds the Activity [Intent] opened when the user taps an RF04 push notification.
 * Implemented in `presentation/navigation` so `data` never imports `presentation`.
 */
fun interface PushTapIntentFactory {
    fun create(type: NotificationType, tripId: TripId): Intent
}
