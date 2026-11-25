package com.example.uniride.presentation.publish

import androidx.annotation.StringRes
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.presentation.common.ContentState
import java.time.LocalDate
import java.time.LocalTime

/** The fields of the form C2 that can show an error. */
enum class PublishField { CAMPUS, MEETING_POINT, NEIGHBORHOOD, DEPARTURE, SEATS, CONTRIBUTION }

/** What the driver has typed or chosen so far. */
data class PublishTripForm(
    val campus: CampusCode? = null,
    val direction: Direction = Direction.IDA,
    val meetingPoint: String? = null,
    val neighborhood: String = "",
    val reference: String = "",
    val departureDate: LocalDate? = null,
    val departureTime: LocalTime? = null,
    val arrivalTime: LocalTime? = null,
    val seats: Int = DEFAULT_SEATS,
    /** Only digits, at most [MAX_CONTRIBUTION_DIGITS], so it always fits in an `Int`. */
    val contribution: String = "",
) {
    companion object {
        const val DEFAULT_SEATS = 3
        const val MAX_CONTRIBUTION_DIGITS = 6
    }
}

/** State of C2 Publicar un viaje (RF02). */
data class PublishTripUiState(
    /** The campuses of the selector; without them the form cannot be filled. */
    val campuses: ContentState<List<Campus>> = ContentState.Loading,
    val form: PublishTripForm = PublishTripForm(),
    /** Text of the error of each field that is wrong and the user already left or submitted. */
    val errors: Map<PublishField, Int> = emptyMap(),
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
) {
    /** Meeting points of the chosen campus; empty until a campus is chosen. */
    val meetingPoints: List<String>
        get() = (campuses as? ContentState.Success)?.data
            ?.firstOrNull { it.code == form.campus }
            ?.meetingPoints
            .orEmpty()
}

sealed interface PublishTripAction {
    data class SelectCampus(val campus: CampusCode) : PublishTripAction
    data class SelectDirection(val direction: Direction) : PublishTripAction
    data class SelectMeetingPoint(val meetingPoint: String) : PublishTripAction
    data class ChangeNeighborhood(val value: String) : PublishTripAction
    data class ChangeReference(val value: String) : PublishTripAction
    data class ChangeDepartureDate(val date: LocalDate) : PublishTripAction
    data class ChangeDepartureTime(val time: LocalTime) : PublishTripAction

    /** `null` clears the optional arrival time. */
    data class ChangeArrivalTime(val time: LocalTime?) : PublishTripAction
    data class ChangeSeats(val seats: Int) : PublishTripAction
    data class ChangeContribution(val value: String) : PublishTripAction

    /** The user left [field]: from now on its error is shown. */
    data class FieldLeft(val field: PublishField) : PublishTripAction
    data object Submit : PublishTripAction
    data object RetryCampuses : PublishTripAction
}

sealed interface PublishTripEffect {
    /** Published: show [messageRes] and go back right away, without waiting for the Snackbar. */
    data class Published(@param:StringRes val messageRes: Int) : PublishTripEffect

    /** Message for the user; the form stays. */
    data class ShowMessage(@param:StringRes val messageRes: Int) : PublishTripEffect
}
