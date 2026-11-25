package com.example.uniride.data.sync

/** Asks for the pending rows to be sent as soon as there is connection (CONVENCIONES §6.2). */
interface SyncScheduler {
    fun schedule()
}
