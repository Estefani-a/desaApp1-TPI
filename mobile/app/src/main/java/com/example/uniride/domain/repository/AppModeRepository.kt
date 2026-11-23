package com.example.uniride.domain.repository

import com.example.uniride.domain.model.AppMode
import kotlinx.coroutines.flow.Flow

/** Mode (passenger or driver) the user chose last, kept between sessions. */
interface AppModeRepository {

    fun observeAppMode(): Flow<AppMode>

    suspend fun setAppMode(mode: AppMode)
}
