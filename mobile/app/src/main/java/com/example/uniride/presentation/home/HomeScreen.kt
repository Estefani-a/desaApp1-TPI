package com.example.uniride.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.navigation.PlaceholderAction
import com.example.uniride.presentation.navigation.PlaceholderScreen
import com.example.uniride.ui.theme.UniRideTheme

/**
 * Dynamic start. Passenger: B6 card of the active trip or an invitation to explore. Driver:
 * C1 without a trip or C4 with requests. Placeholder until RF04.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.appMode) {
        AppMode.PASAJERO -> PlaceholderScreen(
            titleRes = R.string.home_passenger_title,
            modifier = modifier,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_my_trip) { onAction(HomeAction.OpenMyTrip) },
            ),
        )

        AppMode.CONDUCTOR -> PlaceholderScreen(
            titleRes = R.string.home_driver_title,
            modifier = modifier,
            actions = listOf(
                PlaceholderAction(R.string.placeholder_action_publish) { onAction(HomeAction.PublishTrip) },
                PlaceholderAction(R.string.placeholder_action_driver_panel) { onAction(HomeAction.OpenDriverPanel) },
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPassengerPreview() {
    UniRideTheme {
        HomeScreen(state = HomeUiState(AppMode.PASAJERO), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenDriverPreview() {
    UniRideTheme {
        HomeScreen(state = HomeUiState(AppMode.CONDUCTOR), onAction = {})
    }
}
