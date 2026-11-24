package com.example.uniride.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import java.time.LocalTime

/**
 * "Sin conexión" banner of every screen (12.1). It goes on top of the content, which keeps
 * showing the data saved in Room. [lastUpdatedAt] is the time of the last successful refresh,
 * or `null` if it is not known.
 */
@Composable
fun OfflineBanner(
    lastUpdatedAt: LocalTime?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The title says the same: the icon is decorative
            Icon(
                painter = painterResource(R.drawable.ic_cloud_off),
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconMedium),
            )
            Column {
                Text(
                    text = stringResource(R.string.common_offline_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (lastUpdatedAt != null) {
                        stringResource(R.string.common_offline_message_updated_at, formatTime(lastUpdatedAt))
                    } else {
                        stringResource(R.string.common_offline_message)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineBannerPreview() {
    UniRideTheme { OfflineBanner(lastUpdatedAt = LocalTime.of(21, 40)) }
}

@Preview(showBackground = true)
@Composable
private fun OfflineBannerWithoutTimePreview() {
    UniRideTheme { OfflineBanner(lastUpdatedAt = null) }
}
