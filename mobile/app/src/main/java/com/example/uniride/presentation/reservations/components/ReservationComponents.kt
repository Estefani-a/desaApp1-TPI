package com.example.uniride.presentation.reservations.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.presentation.common.StatusChip
import com.example.uniride.presentation.common.StatusTone
import com.example.uniride.presentation.common.labelRes
import com.example.uniride.presentation.common.tone
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

/** A label and its value, one under the other ("Salida" / "Hoy a las 22:15"). */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Status of a reservation, and "Pendiente de sincronizar" while it was not sent (12.1). */
@Composable
fun ReservationStatusRow(status: ReservationStatus, isPendingSync: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(text = stringResource(status.labelRes), tone = status.tone)
        if (isPendingSync) {
            StatusChip(
                text = stringResource(R.string.common_pending_sync),
                tone = StatusTone.INFO,
                iconRes = R.drawable.ic_schedule,
            )
        }
    }
}

/**
 * Why the server did not accept a change (for example, "No se puede cancelar un viaje que ya
 * empezó"). It stays until the user taps "Entendido".
 */
@Composable
fun RejectionNotice(@StringRes messageRes: Int, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceL)) {
            Text(text = stringResource(messageRes), style = MaterialTheme.typography.bodyMedium)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(R.string.rejection_dismiss))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RejectionNoticePreview() {
    UniRideTheme {
        Column(Modifier.padding(Dimens.SpaceL), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            ReservationStatusRow(ReservationStatus.ACEPTADA, isPendingSync = true)
            DetailRow(label = "Salida", value = "Hoy a las 22:15")
            RejectionNotice(R.string.rejection_cancel_departed, onDismiss = {})
        }
    }
}
