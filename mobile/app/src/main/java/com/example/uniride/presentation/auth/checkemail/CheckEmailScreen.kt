package com.example.uniride.presentation.auth.checkemail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.presentation.auth.components.InlineFeedbackBanner
import com.example.uniride.presentation.common.LabeledTextField
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

@Composable
fun CheckEmailRoute(
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: CheckEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CheckEmailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onGoToLogin = onGoToLogin,
    )
}

@Composable
fun CheckEmailScreen(
    state: CheckEmailUiState,
    onAction: (CheckEmailAction) -> Unit,
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.auth_check_email_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.isOffline) {
                OfflineBanner(lastUpdatedAt = null)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceXl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mail_outline),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.IconLarge),
                )

                Text(
                    text = stringResource(R.string.auth_check_email_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = if (state.email.isNotBlank()) {
                        stringResource(R.string.auth_check_email_message_with_address, state.email)
                    } else {
                        stringResource(R.string.auth_check_email_message)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                state.feedbackMessageRes?.let { messageRes ->
                    InlineFeedbackBanner(
                        message = stringResource(messageRes),
                        isError = state.isFeedbackError,
                    )
                }

                LabeledTextField(
                    value = state.email,
                    onValueChange = { onAction(CheckEmailAction.EmailChanged(it)) },
                    label = stringResource(R.string.auth_field_email_label),
                    placeholder = stringResource(R.string.auth_field_email_placeholder),
                    error = state.emailErrorRes?.let { stringResource(it) },
                    enabled = !state.isResending,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false,
                    ),
                )

                PrimaryButton(
                    text = stringResource(R.string.auth_check_email_resend),
                    onClick = { onAction(CheckEmailAction.ResendActivation) },
                    isLoading = state.isResending,
                )

                OutlinedButton(
                    onClick = onGoToLogin,
                    enabled = !state.isResending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(R.string.auth_check_email_go_login))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckEmailScreenPreview() {
    UniRideTheme {
        CheckEmailScreen(
            state = CheckEmailUiState(email = "jperez@uade.edu.ar"),
            onAction = {},
            onBack = {},
            onGoToLogin = {},
        )
    }
}
