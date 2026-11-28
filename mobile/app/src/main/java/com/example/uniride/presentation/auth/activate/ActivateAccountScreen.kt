package com.example.uniride.presentation.auth.activate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ErrorState
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

@Composable
fun ActivateAccountRoute(
    onActivated: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: ActivateAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ActivateAccountEffect.NavigateToMain -> onActivated()
            }
        }
    }

    ActivateAccountScreen(
        state = state,
        onAction = viewModel::onAction,
        onGoToLogin = onGoToLogin,
    )
}

/** A5 Activar cuenta (RF01). Consumes the email deep link token and starts the student's session. */
@Composable
fun ActivateAccountScreen(
    state: ActivateAccountUiState,
    onAction: (ActivateAccountAction) -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.auth_activate_title),
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        when (val content = state.content) {
            ContentState.Loading, ContentState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Dimens.SpaceXl),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.auth_activate_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            is ContentState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Dimens.SpaceXl),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.IconLarge),
                    )
                    Text(
                        text = stringResource(R.string.auth_activate_success),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            is ContentState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    ErrorState(
                        message = stringResource(content.messageRes),
                        onRetry = if (content.canRetry) {
                            { onAction(ActivateAccountAction.Retry) }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.auth_activate_go_login),
                        onClick = onGoToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.ScreenPadding),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivateAccountLoadingPreview() {
    UniRideTheme {
        ActivateAccountScreen(
            state = ActivateAccountUiState(content = ContentState.Loading),
            onAction = {},
            onGoToLogin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivateAccountSuccessPreview() {
    UniRideTheme {
        ActivateAccountScreen(
            state = ActivateAccountUiState(content = ContentState.Success(Unit)),
            onAction = {},
            onGoToLogin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivateAccountErrorPreview() {
    UniRideTheme {
        ActivateAccountScreen(
            state = ActivateAccountUiState(
                content = ContentState.Error(
                    messageRes = R.string.auth_activate_invalid_token,
                    canRetry = false,
                ),
            ),
            onAction = {},
            onGoToLogin = {},
        )
    }
}
