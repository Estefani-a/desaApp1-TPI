package com.example.uniride.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.uniride.data.local.dao.CampusDao
import com.example.uniride.data.local.dao.ReservationDao
import com.example.uniride.data.local.dao.TripDao
import com.example.uniride.data.local.dao.UserDao
import com.example.uniride.data.local.entity.CampusEntity
import com.example.uniride.data.local.entity.ReservationEntity
import com.example.uniride.data.local.entity.TripEntity
import com.example.uniride.data.local.entity.UserEntity

/** The local database: the single source of truth for the UI (CONVENCIONES §6.1). */
@Database(
    entities = [
        UserEntity::class,
        TripEntity::class,
        ReservationEntity::class,
        CampusEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class UniRideDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tripDao(): TripDao
    abstract fun reservationDao(): ReservationDao
    abstract fun campusDao(): CampusDao
}
