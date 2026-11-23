package com.example.uniride.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniride.R
import com.example.uniride.domain.model.AppMode
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Bottom bar: mode selector (pasajero / conductor) on top of the tabs of that mode. The
 * selector only appears when the user can switch ([availableModes] has more than one).
 * Stateless: the shell decides the mode and the selected tab.
 */
@Composable
fun UniRideBottomBar(
    appMode: AppMode,
    availableModes: Set<AppMode>,
    selected: TopLevelDestination?,
    onModeChange: (AppMode) -> Unit,
    onTabClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            if (availableModes.size > 1) {
                ModeSelector(
                    appMode = appMode,
                    modes = availableModes,
                    onModeChange = onModeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            NavigationBar {
                TopLevelDestination.forMode(appMode).forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selected,
                        onClick = { onTabClick(destination) },
                        // The label already names the tab: the icon is decorative
                        icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(
    appMode: AppMode,
    modes: Set<AppMode>,
    onModeChange: (AppMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = AppMode.entries.filter { it in modes }
    val description = stringResource(R.string.nav_mode_selector_description)
    SingleChoiceSegmentedButtonRow(modifier = modifier.semantics { contentDescription = description }) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == appMode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(mode.labelRes))
            }
        }
    }
}

private val AppMode.labelRes: Int
    get() = when (this) {
        AppMode.PASAJERO -> R.string.nav_mode_passenger
        AppMode.CONDUCTOR -> R.string.nav_mode_driver
    }

@Preview(showBackground = true)
@Composable
private fun UniRideBottomBarPassengerPreview() {
    UniRideTheme {
        UniRideBottomBar(
            appMode = AppMode.PASAJERO,
            availableModes = AppMode.entries.toSet(),
            selected = TopLevelDestination.FEED,
            onModeChange = {},
            onTabClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UniRideBottomBarPassengerOnlyPreview() {
    UniRideTheme {
        UniRideBottomBar(
            appMode = AppMode.PASAJERO,
            availableModes = setOf(AppMode.PASAJERO),
            selected = TopLevelDestination.HOME,
            onModeChange = {},
            onTabClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UniRideBottomBarDriverPreview() {
    UniRideTheme {
        UniRideBottomBar(
            appMode = AppMode.CONDUCTOR,
            availableModes = AppMode.entries.toSet(),
            selected = TopLevelDestination.HOME,
            onModeChange = {},
            onTabClick = {},
        )
    }
}
