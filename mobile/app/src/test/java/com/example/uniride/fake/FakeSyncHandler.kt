package com.example.uniride.fake

import com.example.uniride.data.sync.SyncHandler
import com.example.uniride.data.sync.SyncOutcome

/** [SyncHandler] whose answer for each call is decided by [behavior], which receives the call number. */
class FakeSyncHandler(private val behavior: (call: Int) -> SyncOutcome) : SyncHandler {

    var calls = 0
        private set

    override suspend fun syncPending(): SyncOutcome = behavior(calls++)

    companion object {
        fun always(outcome: SyncOutcome) = FakeSyncHandler { outcome }

        /** Answers with [outcomes] in order and repeats the last one. */
        fun sequence(vararg outcomes: SyncOutcome) =
            FakeSyncHandler { call -> outcomes[minOf(call, outcomes.lastIndex)] }

        fun throwing(error: Throwable) = FakeSyncHandler { throw error }
    }
}
