package com.example.uniride.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.uniride.domain.model.PreferredMode

/**
 * The logged-in student (preentrega 11.1). Only one row at a time: the table is cleared on
 * logout. The rating and the vehicle are flattened into columns; the vehicle columns are all
 * `null` when the student does not drive.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val career: String,
    val phone: String,
    val preferredMode: PreferredMode,
    /** `null` while nobody rated the user. */
    val ratingAvg: Double?,
    val ratingCount: Int,
    val vehicleBrand: String?,
    val vehicleModel: String?,
    val vehicleColor: String?,
    val vehiclePlate: String?,
)
