package com.example.uniride.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniride.R
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Id of the trip the placeholders navigate to. It disappears together with them when each
 * RF reads the real trips from Room.
 */
const val PLACEHOLDER_TRIP_ID = "placeholder-trip"

/** Button of a placeholder that goes to another screen. */
data class PlaceholderAction(@StringRes val labelRes: Int, val onClick: () -> Unit)

/**
 * Temporary screen of G3: shows its name and buttons to walk the navigation. Each RF replaces
 * it with its real `XxxRoute`. The Figma code of each screen is in the KDoc of its route.
 */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    actions: List<PlaceholderAction> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.placeholder_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        actions.forEach { action ->
            OutlinedButton(
                onClick = action.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(action.labelRes))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    UniRideTheme {
        PlaceholderScreen(
            titleRes = R.string.feed_title,
            actions = listOf(PlaceholderAction(R.string.placeholder_action_trip_detail) {}),
        )
    }
}
