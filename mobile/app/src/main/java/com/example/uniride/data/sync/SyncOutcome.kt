package com.example.uniride.data.sync

/** What a [SyncHandler] reports after one pass over its pending rows. */
enum class SyncOutcome {
    /** Nothing was pending, so nothing was sent. */
    DONE,

    /**
     * At least one pending row was sent or resolved (accepted or rejected by the server) and
     * none failed. Something may have been saved while it ran, so [SyncRunner] does another pass.
     */
    PROGRESS,

    /** It failed for a reason that may go away (no connection, timeout, 5xx): try again later. */
    RETRY,

    /** The session is not valid (401): retrying makes no sense until the user logs in again. */
    STOP,
}
