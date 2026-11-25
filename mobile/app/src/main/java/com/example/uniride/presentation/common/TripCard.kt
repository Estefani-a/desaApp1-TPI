package com.example.uniride.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniride.R
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import java.time.LocalDate

/**
 * Card of a trip in a list, as in B1 Explorar of the guide: departure and arrival times next to
 * the route (filled dot = origin, hollow dot = destination), the contribution on the right and the
 * driver with the free seats below. Used by the feed and by the start screens. It only shows
 * public data (RNF07): no plate, no phone.
 *
 * @param campusName name of the campus ("Sede Monserrat"); if the campuses are not loaded yet,
 * the code of the trip is shown
 * @param isPendingSync the trip was created or changed on this phone and is not on the server yet
 */
@Composable
fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    campusName: String? = null,
    isPendingSync: Boolean = false,
    today: LocalDate = LocalDate.now(BusinessZone),
) {
    val departure = formatTime(trip.schedule.departureTime)
    val openLabel = stringResource(R.string.trip_card_open_description, departure)
    val campus = campusName ?: trip.campus.value
    val campusStop = Stop(title = campus, detail = trip.route.meetingPoint)
    val neighborhoodStop = Stop(title = trip.route.destinationNeighborhood, detail = trip.route.destinationReference)
    val (origin, destination) = if (trip.route.direction == Direction.IDA) {
        neighborhoodStop to campusStop
    } else {
        campusStop to neighborhoodStop
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = openLabel, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceL)) {
            if (trip.schedule.departureDate != today) {
                Text(
                    text = dateLabel(trip.schedule.departureDate, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.SpaceS),
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    StopRow(time = departure, stop = origin, isOrigin = true)
                    RouteLine()
                    StopRow(
                        time = trip.schedule.estimatedArrivalTime?.let(::formatTime),
                        stop = destination,
                        isOrigin = false,
                    )
                }
                Price(trip.contributionPerSeat)
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = Dimens.SpaceM),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(trip.driver.displayName)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Dimens.SpaceS),
                ) {
                    Text(
                        text = trip.driver.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = trip.driver.rating?.let {
                            stringResource(R.string.trip_card_verified_rating, formatRating(it))
                        } ?: stringResource(R.string.trip_card_verified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SeatsChip(trip)
            }

            if (isPendingSync) {
                PendingSyncChip(modifier = Modifier.padding(top = Dimens.SpaceS))
            }
        }
    }
}

private data class Stop(val title: String, val detail: String?)

private val TimeColumnWidth = 48.dp
private val DotSize = 10.dp

@Composable
private fun StopRow(time: String?, stop: Stop, isOrigin: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = time.orEmpty(),
            style = if (isOrigin) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (isOrigin) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(TimeColumnWidth),
        )
        // Same height as the first line of text, so the dot is centered on the name of the stop
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            RouteDot(filled = isOrigin)
        }
        Column(modifier = Modifier.padding(start = Dimens.SpaceS)) {
            Text(
                text = stop.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (stop.detail != null) {
                Text(
                    text = stop.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Vertical line between the two dots of the route. */
@Composable
private fun RouteLine() {
    Box(
        modifier = Modifier
            .padding(start = TimeColumnWidth + (DotSize - 2.dp) / 2)
            .width(2.dp)
            .height(Dimens.SpaceM)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun RouteDot(filled: Boolean) {
    val color = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(DotSize)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(color) else Modifier.border(2.dp, color, CircleShape)),
    )
}

@Composable
private fun Price(contributionPerSeat: Int) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = Dimens.SpaceS)) {
        if (contributionPerSeat > 0) {
            Text(text = formatMoney(contributionPerSeat), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.trip_card_per_seat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(text = stringResource(R.string.trip_card_contribution_free), style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Circle with the initials of "Lucía F." → "LF". Decorative: the name is next to it. */
@Composable
fun InitialsAvatar(displayName: String, modifier: Modifier = Modifier) {
    val initials = displayName.split(' ', '.')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SeatsChip(trip: Trip) {
    when {
        trip.status == TripStatus.ABIERTO && trip.availableSeats > 0 -> StatusChip(
            text = pluralStringResource(R.plurals.trip_card_seats_available, trip.availableSeats, trip.availableSeats),
            tone = StatusTone.NEUTRAL,
            iconRes = R.drawable.ic_nav_person,
            outlined = true,
        )
        trip.status == TripStatus.ABIERTO || trip.status == TripStatus.COMPLETO ->
            StatusChip(text = stringResource(R.string.trip_card_full), tone = StatusTone.WARNING)
        else -> TripStatusChip(trip.status)
    }
}

@Preview(showBackground = true)
@Composable
private fun TripCardPreview() {
    UniRideTheme {
        Column(Modifier.padding(Dimens.SpaceL)) {
            TripCard(trip = PreviewData.trip, onClick = {}, campusName = "Sede Monserrat")
            Spacer(Modifier.height(Dimens.SpaceM))
            TripCard(trip = PreviewData.fullTrip, onClick = {}, campusName = "Sede Belgrano", isPendingSync = true)
        }
    }
}
