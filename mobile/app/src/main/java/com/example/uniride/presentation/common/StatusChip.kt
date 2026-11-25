package com.example.uniride.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniride.R
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.TripStatus
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

/** Meaning of a [StatusChip]. Each tone is a pair of colors of the theme. */
enum class StatusTone { NEUTRAL, INFO, POSITIVE, WARNING, NEGATIVE }

/**
 * Small label with a state (with [outlined], the light version of the guide, as "2 libres" on a
 * trip card): "Abierto", "Pendiente", "Pendiente de sincronizar"… It is not
 * clickable. The meaning is always in the [text], never only in the color (RNF01).
 */
@Composable
fun StatusChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    outlined: Boolean = false,
) {
    val (container, content) = MaterialTheme.colorScheme.colorsFor(tone)
    Surface(
        color = if (outlined) MaterialTheme.colorScheme.surface else container,
        contentColor = if (outlined) MaterialTheme.colorScheme.onSurface else content,
        shape = MaterialTheme.shapes.small,
        border = if (outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.IconSmall),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun TripStatusChip(status: TripStatus, modifier: Modifier = Modifier) {
    StatusChip(text = stringResource(status.labelRes), tone = status.tone, modifier = modifier)
}

@Composable
fun ReservationStatusChip(status: ReservationStatus, modifier: Modifier = Modifier) {
    StatusChip(text = stringResource(status.labelRes), tone = status.tone, modifier = modifier)
}

/** "Pendiente de sincronizar": the change is saved on the phone and goes out with connection. */
@Composable
fun PendingSyncChip(modifier: Modifier = Modifier) {
    StatusChip(
        text = stringResource(R.string.common_pending_sync),
        tone = StatusTone.INFO,
        iconRes = R.drawable.ic_schedule,
        modifier = modifier,
    )
}

private fun ColorScheme.colorsFor(tone: StatusTone): Pair<Color, Color> = when (tone) {
    StatusTone.NEUTRAL -> surfaceContainerHighest to onSurfaceVariant
    StatusTone.INFO -> secondaryContainer to onSecondaryContainer
    // The guide marks what is confirmed with the dark ink, like the main actions
    StatusTone.POSITIVE -> primary to onPrimary
    StatusTone.WARNING -> tertiaryContainer to onTertiaryContainer
    StatusTone.NEGATIVE -> errorContainer to onErrorContainer
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    UniRideTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            TripStatusChip(TripStatus.ABIERTO)
            ReservationStatusChip(ReservationStatus.PENDIENTE)
            PendingSyncChip()
        }
    }
}
