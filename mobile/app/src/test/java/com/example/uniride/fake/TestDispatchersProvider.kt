package com.example.uniride.fake

import com.example.uniride.core.dispatcher.DispatchersProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

/** Test [DispatchersProvider]: io, default and main all run on the same [dispatcher]. */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatchersProvider(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : DispatchersProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}
