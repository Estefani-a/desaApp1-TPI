package com.example.uniride.domain.model

/** The logged-in student. Other students appear as [Participant]. */
data class User(
    val id: UserId,
    val email: String,
    val profile: Profile,
    /** `null` while nobody rated the user. */
    val rating: Rating?,
) {
    /** Only a user with a vehicle can offer trips. */
    val canDrive: Boolean get() = profile.vehicle != null
}

/**
 * What a student tells about themselves. The same data is used to create the account and
 * to edit it later; the email is not here because it never changes.
 */
data class Profile(
    val fullName: String,
    val career: String,
    val phone: String,
    val preferredMode: PreferredMode,
    /** `null` if the student does not drive. */
    val vehicle: Vehicle?,
)

data class Vehicle(
    val brand: String,
    val model: String,
    val color: String,
    val plate: String,
)

/** Average of the stars received and how many ratings it is based on. */
data class Rating(
    val average: Double,
    val count: Int,
)

/**
 * Another student as seen by the user on a shared trip: the driver of a trip or the passenger
 * of a request. [phone] is only known once both travel together (accepted reservation, RNF07).
 */
data class Participant(
    val id: UserId,
    /** First name and initial of the last name, "Lucía F.". */
    val displayName: String,
    val rating: Double?,
    val phone: String?,
)
