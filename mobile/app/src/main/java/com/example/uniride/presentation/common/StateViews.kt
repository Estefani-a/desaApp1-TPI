package com.example.uniride.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniride.R
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

/** Centered spinner while the first data of a screen arrives. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.common_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Nothing to show yet: what happened and, optionally, what the user can do about it (for
 * example, clear the filters of the feed).
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    @DrawableRes iconRes: Int = R.drawable.ic_nav_search,
) {
    MessageState(
        iconRes = iconRes,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        message = message,
        actionLabel = actionLabel,
        onAction = onAction,
        modifier = modifier,
    )
}

/** Error with a retry button. Without [onRetry] the button is hidden (retrying would not help). */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    title: String = stringResource(R.string.common_error_title),
) {
    MessageState(
        iconRes = R.drawable.ic_error_outline,
        iconTint = MaterialTheme.colorScheme.error,
        title = title,
        message = message,
        actionLabel = onRetry?.let { stringResource(R.string.common_retry) },
        onAction = onRetry,
        // TalkBack reads the error when it appears, not only when the user gets to it
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun MessageState(
    @DrawableRes iconRes: Int,
    iconTint: Color,
    title: String,
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.SpaceXl, vertical = Dimens.SpaceXxl),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The title already says what happened: the icon is decorative
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Dimens.IconLarge),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.padding(top = Dimens.SpaceS),
                contentPadding = PaddingValues(
                    horizontal = Dimens.SpaceXl,
                    vertical = 14.dp,
                ),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    UniRideTheme { LoadingState() }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    UniRideTheme {
        EmptyState(
            title = stringResource(R.string.common_empty_title),
            message = stringResource(R.string.common_empty_message),
            actionLabel = stringResource(R.string.common_retry),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    UniRideTheme {
        ErrorState(message = stringResource(R.string.common_error_server), onRetry = {})
    }
}
