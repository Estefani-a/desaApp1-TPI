package com.example.uniride.domain.usecase.trip

import com.example.uniride.core.result.AppResult
import com.example.uniride.core.result.DataError
import com.example.uniride.domain.model.TripDraft
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.CampusRepository
import com.example.uniride.domain.repository.TripRepository
import com.example.uniride.domain.validation.TripDraftError
import com.example.uniride.domain.validation.TripDraftRule
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * What happened when the driver tried to publish. It is not an `AppResult` because a draft with
 * wrong fields is not a [DataError] (those come from the data layer): the form needs the set of
 * fields to mark, and a [DataError] has no place for it.
 */
sealed interface PublishTripResult {
    /** Saved on the phone; it is sent to the server when there is connection. */
    data class Published(val tripId: TripId) : PublishTripResult

    /** Some fields are wrong; nothing was saved. */
    data class Invalid(val errors: Set<TripDraftError>) : PublishTripResult

    /** The draft was valid but the repository could not save it (for example `UNAUTHORIZED`). */
    data class Failed(val error: DataError) : PublishTripResult
}

/**
 * Publishes a trip (RF02). It checks the draft with [TripDraftRule] against the saved campuses
 * and only then asks the repository, so a wrong draft never reaches it. Works offline.
 */
class PublishTripUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val campusRepository: CampusRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: TripDraft): PublishTripResult {
        val campus = campusRepository.observeCampuses().first().firstOrNull { it.code == draft.campus }
        val errors = TripDraftRule.evaluate(draft, campus, LocalDateTime.now(clock))
        if (errors.isNotEmpty()) return PublishTripResult.Invalid(errors)

        return when (val result = tripRepository.publish(draft)) {
            is AppResult.Success -> PublishTripResult.Published(result.data)
            is AppResult.Failure -> PublishTripResult.Failed(result.error)
        }
    }
}
