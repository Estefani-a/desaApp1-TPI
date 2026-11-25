package com.example.uniride.core.dispatcher

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Source of coroutine dispatchers (CONVENCIONES §8). Classes receive this instead of using
 * `Dispatchers.IO` directly, so tests can swap in a test dispatcher.
 */
interface DispatchersProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

/** Production implementation backed by the real [Dispatchers]. */
class DefaultDispatchersProvider @Inject constructor() : DispatchersProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
