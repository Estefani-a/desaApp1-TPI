package com.example.uniride.data.sync

import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Runs every registered [SyncHandler] and combines their results. It has no Android classes, so
 * it is tested on the JVM.
 *
 * The scheduler enqueues the work with `KEEP`: if something is saved while the worker runs, the
 * new request is dropped. To not leave that row unsent, the runner repeats the pass while some
 * handler reports [SyncOutcome.PROGRESS], up to [maxPasses]. If the limit is reached and there
 * is still progress, it returns [SyncOutcome.RETRY] so WorkManager runs it again later.
 *
 * A handler that throws does not stop the others: it counts as [SyncOutcome.RETRY].
 * [CancellationException] is always rethrown.
 */
class SyncRunner(
    private val handlers: Set<SyncHandler>,
    private val maxPasses: Int,
) {
    @Inject
    constructor(handlers: Set<@JvmSuppressWildcards SyncHandler>) : this(handlers, DEFAULT_MAX_PASSES)

    init {
        require(maxPasses >= 1) { "maxPasses must be at least 1" }
    }

    /** Returns [SyncOutcome.STOP], else [SyncOutcome.RETRY], else [SyncOutcome.DONE]. */
    suspend fun run(): SyncOutcome {
        repeat(maxPasses) {
            val outcomes = handlers.map { runHandler(it) }
            when {
                SyncOutcome.STOP in outcomes -> return SyncOutcome.STOP
                SyncOutcome.RETRY in outcomes -> return SyncOutcome.RETRY
                SyncOutcome.PROGRESS !in outcomes -> return SyncOutcome.DONE
            }
        }
        return SyncOutcome.RETRY
    }

    private suspend fun runHandler(handler: SyncHandler): SyncOutcome =
        try {
            handler.syncPending()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncOutcome.RETRY
        }

    companion object {
        const val DEFAULT_MAX_PASSES = 3
    }
}
