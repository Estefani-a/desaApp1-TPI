package com.example.uniride.data.sync

/**
 * Sends the pending rows (`isSync = false`) of one area to the server (CONVENCIONES §6.2).
 * Each area implements one and registers it in its Hilt module; the generic sync mechanism
 * knows nothing about trips or reservations.
 *
 * Rules for an implementation, for each pending row:
 * - Call the server with `safeApiCall` and `PUT` with the row's id, which is idempotent: sending
 *   the same row twice is safe.
 * - Success: mark the row as synced (`markSynced`) and go on with the next one.
 * - `NO_CONNECTION`, `TIMEOUT` or `SERVER`: stop the pass and return [SyncOutcome.RETRY]
 *   without touching the row.
 * - `UNAUTHORIZED`: stop and return [SyncOutcome.STOP].
 * - Any other error is a rejection by the server (`VALIDATION`, `CONFLICT`, `NOT_FOUND`, and
 *   `UNKNOWN`, where a 403 ends up): update the row with the real state so the user sees what
 *   happened, mark it as synced so it is not sent again, and go on with the rest.
 *
 * Return [SyncOutcome.PROGRESS] if at least one row was sent or resolved, [SyncOutcome.DONE]
 * if there was nothing pending. Do not catch `CancellationException`.
 */
interface SyncHandler {
    suspend fun syncPending(): SyncOutcome
}
