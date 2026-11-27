package com.example.uniride.data.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Supplies the current FCM registration token, or `null` when Firebase is not configured. */
interface FcmTokenProvider {
    suspend fun currentToken(): String?
}

/**
 * Reads the device token from [FirebaseMessaging] only when a [FirebaseApp] is initialized
 * (i.e. `google-services.json` was present at build time). Never logs the token.
 */
@Singleton
class FirebaseFcmTokenProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : FcmTokenProvider {

    override suspend fun currentToken(): String? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        return try {
            suspendCancellableCoroutine { continuation ->
                @Suppress("DEPRECATION") // Backend API.md §4.8 uses FCM registration tokens
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    val token = if (task.isSuccessful) {
                        task.result?.trim()?.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                    continuation.resume(token)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
