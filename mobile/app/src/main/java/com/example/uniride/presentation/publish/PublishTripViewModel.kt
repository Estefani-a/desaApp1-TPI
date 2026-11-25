package com.example.uniride.presentation.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripRoute
import com.example.uniride.domain.model.TripSchedule
import com.example.uniride.domain.usecase.campus.ObserveCampusesUseCase
import com.example.uniride.domain.usecase.campus.RefreshCampusesUseCase
import com.example.uniride.domain.usecase.connectivity.ObserveConnectivityUseCase
import com.example.uniride.domain.usecase.trip.PublishTripResult
import com.example.uniride.domain.usecase.trip.PublishTripUseCase
import com.example.uniride.domain.validation.TripDraftRule
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.messageRes
import com.example.uniride.presentation.common.toContentError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * C2 Publicar un viaje (RF02). The error of a field shows up when the user leaves it or taps
 * "Publicar viaje", not while typing for the first time. Publishing works offline: the trip is
 * saved on the phone and sent later, so the form stays usable without connection.
 *
 * `TripRepository.publish` does not use the network, so a `VALIDATION` failure can only come
 * from its own check that the driver has a vehicle loaded in the profile; the other fields were
 * already checked by `TripDraftRule`.
 */
@HiltViewModel
class PublishTripViewModel @Inject constructor(
    private val observeCampuses: ObserveCampusesUseCase,
    private val refreshCampuses: RefreshCampusesUseCase,
    private val publishTrip: PublishTripUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val input = MutableStateFlow(
        InputState(form = PublishTripForm(departureDate = LocalDate.now(clock))),
    )
    private val campusRefresh = MutableStateFlow(CampusRefresh(isRefreshing = true))
    private var campusJob: Job? = null

    private val _effects = Channel<PublishTripEffect>(Channel.BUFFERED)
    val effects: Flow<PublishTripEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<PublishTripUiState> = combine(
        observeCampuses(),
        observeConnectivity(),
        input,
        campusRefresh,
    ) { campuses, isOnline, input, refresh ->
        val errors = fieldErrors(input.form, campuses) + input.useCaseErrors
        PublishTripUiState(
            campuses = campusesContent(campuses, refresh),
            form = input.form,
            errors = errors.filterKeys { input.submitted || it in input.touched },
            isSubmitting = input.isSubmitting,
            isOffline = !isOnline,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PublishTripUiState(form = input.value.form),
    )

    init {
        loadCampuses()
    }

    fun onAction(action: PublishTripAction) {
        when (action) {
            is PublishTripAction.SelectCampus -> edit(PublishField.CAMPUS) {
                // The meeting points belong to a campus: another campus, another list
                copy(campus = action.campus, meetingPoint = null)
            }
            is PublishTripAction.SelectDirection -> edit { copy(direction = action.direction) }
            is PublishTripAction.SelectMeetingPoint -> edit(PublishField.MEETING_POINT) {
                copy(meetingPoint = action.meetingPoint)
            }
            is PublishTripAction.ChangeNeighborhood -> edit { copy(neighborhood = action.value) }
            is PublishTripAction.ChangeReference -> edit { copy(reference = action.value) }
            is PublishTripAction.ChangeDepartureDate -> edit(PublishField.DEPARTURE) {
                copy(departureDate = action.date)
            }
            is PublishTripAction.ChangeDepartureTime -> edit(PublishField.DEPARTURE) {
                copy(departureTime = action.time)
            }
            is PublishTripAction.ChangeArrivalTime -> edit { copy(arrivalTime = action.time) }
            is PublishTripAction.ChangeSeats -> edit(PublishField.SEATS) { copy(seats = action.seats) }
            is PublishTripAction.ChangeContribution -> edit {
                copy(
                    contribution = action.value
                        .filter(Char::isDigit)
                        .take(PublishTripForm.MAX_CONTRIBUTION_DIGITS),
                )
            }
            is PublishTripAction.FieldLeft -> input.update { it.copy(touched = it.touched + action.field) }
            PublishTripAction.Submit -> submit()
            PublishTripAction.RetryCampuses -> loadCampuses()
        }
    }

    /** Changes the form. [touches] is a field whose error shows from now on (the selectors). */
    private fun edit(touches: PublishField? = null, change: PublishTripForm.() -> PublishTripForm) {
        input.update { current ->
            val form = current.form.change()
            var touched = current.touched
            if (touches != null) touched += touches
            // A campus change empties the meeting point: its error waits for the next try
            if (form.meetingPoint == null) touched -= PublishField.MEETING_POINT
            current.copy(form = form, touched = touched, useCaseErrors = emptyMap())
        }
    }

    private fun submit() {
        // Atomic: two taps in a row must not publish twice
        var started = false
        input.update {
            started = !it.isSubmitting
            if (started) it.copy(isSubmitting = true, submitted = true, useCaseErrors = emptyMap()) else it
        }
        if (!started) return

        viewModelScope.launch {
            val form = input.value.form
            val campuses = observeCampuses().first()
            val draft = toDraft(form)
            if (draft == null || fieldErrors(form, campuses).isNotEmpty()) {
                // The errors are already on screen (submitted = true)
                input.update { it.copy(isSubmitting = false) }
                return@launch
            }

            // Whether it can be sent now is decided when the driver taps, not when it is saved
            val isOnline = observeConnectivity().first()
            when (val result = publishTrip(draft)) {
                is PublishTripResult.Published -> _effects.send(
                    PublishTripEffect.Published(
                        if (isOnline) R.string.publish_success else R.string.publish_success_offline,
                    ),
                )
                is PublishTripResult.Invalid -> {
                    val errors = result.errors.associate { it.field to it.messageRes }
                    input.update { it.copy(isSubmitting = false, useCaseErrors = errors) }
                }
                is PublishTripResult.Failed -> {
                    input.update { it.copy(isSubmitting = false) }
                    val messageRes = when (result.error) {
                        DataError.VALIDATION -> R.string.publish_error_vehicle
                        else -> result.error.messageRes()
                    }
                    _effects.send(PublishTripEffect.ShowMessage(messageRes))
                }
            }
        }
    }

    private fun loadCampuses() {
        if (campusJob?.isActive == true) return
        campusRefresh.value = CampusRefresh(isRefreshing = true)
        campusJob = viewModelScope.launch {
            val error = (refreshCampuses() as? AppResult.Failure)?.error
            campusRefresh.value = CampusRefresh(isRefreshing = false, lastError = error)
        }
    }

    private fun campusesContent(campuses: List<Campus>, refresh: CampusRefresh): ContentState<List<Campus>> =
        when {
            campuses.isNotEmpty() -> ContentState.Success(campuses)
            refresh.isRefreshing -> ContentState.Loading
            refresh.lastError == DataError.NO_CONNECTION -> ContentState.Error(R.string.publish_campuses_offline)
            refresh.lastError != null -> refresh.lastError.toContentError()
            else -> ContentState.Empty
        }

    /**
     * The fields that are wrong now, shown or not yet. A missing departure or contribution is
     * reported here; the rest comes from [TripDraftRule].
     */
    private fun fieldErrors(form: PublishTripForm, campuses: List<Campus>): Map<PublishField, Int> {
        val errors = mutableMapOf<PublishField, Int>()
        val hasDeparture = form.departureDate != null && form.departureTime != null
        if (!hasDeparture) errors[PublishField.DEPARTURE] = R.string.publish_error_departure_missing
        if (form.contribution.isEmpty()) errors[PublishField.CONTRIBUTION] = R.string.publish_error_contribution

        val campus = campuses.firstOrNull { it.code == form.campus }
        TripDraftRule.evaluate(draftOf(form), campus, LocalDateTime.now(clock)).forEach { error ->
            // Without a departure, "not in the future" would be about the placeholder
            if (hasDeparture || error.field != PublishField.DEPARTURE) {
                errors.putIfAbsent(error.field, error.messageRes)
            }
        }
        return errors
    }

    /** The draft to check; a missing date or time takes a placeholder that [fieldErrors] ignores. */
    private fun draftOf(form: PublishTripForm): TripDraft = TripDraft(
        campus = form.campus ?: CampusCode(""),
        route = TripRoute(
            direction = form.direction,
            meetingPoint = form.meetingPoint.orEmpty(),
            destinationNeighborhood = form.neighborhood.trim(),
            destinationReference = form.reference.trim().ifEmpty { null },
        ),
        schedule = TripSchedule(
            departureDate = form.departureDate ?: LocalDate.now(clock),
            departureTime = form.departureTime ?: LocalTime.MIDNIGHT,
            estimatedArrivalTime = form.arrivalTime,
        ),
        totalSeats = form.seats,
        contributionPerSeat = form.contribution.toIntOrNull() ?: 0,
    )

    /** The draft to publish, or `null` if a campus, a departure or the contribution is missing. */
    private fun toDraft(form: PublishTripForm): TripDraft? =
        if (form.campus == null || form.departureDate == null || form.departureTime == null ||
            form.contribution.toIntOrNull() == null
        ) {
            null
        } else {
            draftOf(form)
        }

    private data class InputState(
        val form: PublishTripForm,
        val touched: Set<PublishField> = emptySet(),
        /** The user tapped "Publicar viaje": every error is shown. */
        val submitted: Boolean = false,
        val isSubmitting: Boolean = false,
        /** Errors that only the use case found; they go away with the next edit. */
        val useCaseErrors: Map<PublishField, Int> = emptyMap(),
    )

    private data class CampusRefresh(
        val isRefreshing: Boolean = false,
        val lastError: DataError? = null,
    )
}
