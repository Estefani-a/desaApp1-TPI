package com.example.uniride.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.uniride.data.local.entity.CampusEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CampusDao {

    @Query("SELECT * FROM campuses ORDER BY name")
    abstract fun observeAll(): Flow<List<CampusEntity>>

    /** Replaces the whole list: the one from the server is always complete. */
    @Transaction
    open suspend fun replaceAll(campuses: List<CampusEntity>) {
        deleteAll()
        insertAll(campuses)
    }

    @Query("DELETE FROM campuses")
    protected abstract suspend fun deleteAll()

    @Insert
    protected abstract suspend fun insertAll(campuses: List<CampusEntity>)
}
