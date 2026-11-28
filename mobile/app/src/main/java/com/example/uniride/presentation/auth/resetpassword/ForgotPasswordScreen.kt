package com.example.uniride.presentation.auth.resetpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.uniride.presentation.common.InlineFeedbackBanner
import com.example.uniride.presentation.common.LabeledTextField
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onBackToLogin = onBackToLogin,
    )
}

/** A6 Recuperar contraseña (RF01). Requests a password reset email without disclosing account existence. */
@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onAction: (ForgotPasswordAction) -> Unit,
    onBack: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.auth_forgot_password_title),
                onBack = onBack,
                windowInsets = WindowInsets(0),
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

            if (state.isSent) {
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
                        text = stringResource(R.string.auth_forgot_password_sent_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.auth_forgot_password_sent_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    PrimaryButton(
                        text = stringResource(R.string.auth_forgot_password_back_login),
                        onClick = onBackToLogin,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
                ) {
                    Text(
                        text = stringResource(R.string.auth_forgot_password_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    state.generalErrorRes?.let { errorRes ->
                        InlineFeedbackBanner(
                            message = stringResource(errorRes),
                            isError = true,
                        )
                    }

                    LabeledTextField(
                        value = state.email,
                        onValueChange = { onAction(ForgotPasswordAction.EmailChanged(it)) },
                        label = stringResource(R.string.auth_field_email_label),
                        placeholder = stringResource(R.string.auth_field_email_placeholder),
                        error = state.emailErrorRes?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                            autoCorrectEnabled = false,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onAction(ForgotPasswordAction.Submit) },
                        ),
                    )

                    PrimaryButton(
                        text = stringResource(R.string.auth_forgot_password_submit),
                        onClick = { onAction(ForgotPasswordAction.Submit) },
                        isLoading = state.isSubmitting,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    UniRideTheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(email = "lfernandez@uade.edu.ar"),
            onAction = {},
            onBack = {},
            onBackToLogin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordErrorOfflinePreview() {
    UniRideTheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(
                email = "lfernandez@gmail.com",
                emailErrorRes = R.string.auth_error_email_domain,
                isOffline = true,
            ),
            onAction = {},
            onBack = {},
            onBackToLogin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordSentPreview() {
    UniRideTheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(isSent = true),
            onAction = {},
            onBack = {},
            onBackToLogin = {},
        )
    }
}
