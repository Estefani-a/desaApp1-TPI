package com.example.uniride.data.mapper

import com.example.uniride.data.local.entity.UserEntity
import com.example.uniride.domain.model.Profile
import com.example.uniride.domain.model.Rating
import com.example.uniride.domain.model.User
import com.example.uniride.domain.model.UserId
import com.example.uniride.domain.model.Vehicle

/** The vehicle exists only if its four columns are filled; otherwise the student does not drive. */
fun UserEntity.toDomain(): User {
    val brand = vehicleBrand
    val model = vehicleModel
    val color = vehicleColor
    val plate = vehiclePlate
    val vehicle = if (brand != null && model != null && color != null && plate != null) {
        Vehicle(brand, model, color, plate)
    } else {
        null
    }
    return User(
        id = UserId(id),
        email = email,
        profile = Profile(fullName, career, phone, preferredMode, vehicle),
        rating = ratingAvg?.let { Rating(it, ratingCount) },
    )
}

fun User.toEntity(): UserEntity = UserEntity(
    id = id.value,
    email = email,
    fullName = profile.fullName,
    career = profile.career,
    phone = profile.phone,
    preferredMode = profile.preferredMode,
    ratingAvg = rating?.average,
    ratingCount = rating?.count ?: 0,
    vehicleBrand = profile.vehicle?.brand,
    vehicleModel = profile.vehicle?.model,
    vehicleColor = profile.vehicle?.color,
    vehiclePlate = profile.vehicle?.plate,
)
