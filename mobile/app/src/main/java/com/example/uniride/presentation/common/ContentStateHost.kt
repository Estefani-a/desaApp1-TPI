package com.example.uniride.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Draws the four cases of [ContentState] the same way on every screen (CONVENCIONES §7.3):
 * [LoadingState], [empty], [ErrorState] (with [onRetry] when the error allows it) and [content].
 *
 * ```
 * ContentStateHost(state.content, onRetry = { onAction(FeedAction.Refresh) }) { trips ->
 *     TripList(trips)
 * }
 * ```
 */
@Composable
fun <T> ContentStateHost(
    state: ContentState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    empty: @Composable () -> Unit = {
        EmptyState(
            title = stringResource(R.string.common_empty_title),
            message = stringResource(R.string.common_empty_message),
        )
    },
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            ContentState.Loading -> LoadingState()
            ContentState.Empty -> empty()
            is ContentState.Error -> ErrorState(
                message = stringResource(state.messageRes),
                onRetry = onRetry.takeIf { state.canRetry },
            )
            is ContentState.Success -> content(state.data)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentStateHostErrorPreview() {
    UniRideTheme {
        ContentStateHost<String>(
            state = ContentState.Error(R.string.common_error_no_connection),
            onRetry = {},
        ) { Text(it) }
    }
}
