package com.example.uniride.presentation.auth.resetpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.presentation.auth.components.InlineFeedbackBanner
import com.example.uniride.presentation.auth.components.PasswordTextField
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

@Composable
fun ResetPasswordRoute(
    onGoToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ResetPasswordScreen(
        state = state,
        onAction = viewModel::onAction,
        onGoToLogin = onGoToLogin,
    )
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordUiState,
    onAction: (ResetPasswordAction) -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UniRideTopBar(title = stringResource(R.string.auth_reset_password_title))
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

            if (state.isSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpaceXl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.IconLarge),
                    )
                    Text(
                        text = stringResource(R.string.auth_reset_password_success_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.auth_reset_password_success_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    PrimaryButton(
                        text = stringResource(R.string.auth_forgot_password_back_login),
                        onClick = onGoToLogin,
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
                        text = stringResource(R.string.auth_reset_password_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    state.generalErrorRes?.let { errorRes ->
                        InlineFeedbackBanner(
                            message = stringResource(errorRes),
                            isError = true,
                        )
                    }

                    PasswordTextField(
                        value = state.newPassword,
                        onValueChange = { onAction(ResetPasswordAction.NewPasswordChanged(it)) },
                        label = stringResource(R.string.auth_field_new_password_label),
                        supportingText = stringResource(R.string.auth_field_password_supporting),
                        error = state.passwordErrorRes?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Next,
                    )

                    PasswordTextField(
                        value = state.confirmPassword,
                        onValueChange = { onAction(ResetPasswordAction.ConfirmPasswordChanged(it)) },
                        label = stringResource(R.string.auth_field_confirm_password_label),
                        error = state.confirmPasswordErrorRes?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = { onAction(ResetPasswordAction.Submit) },
                        ),
                    )

                    PrimaryButton(
                        text = stringResource(R.string.auth_reset_password_submit),
                        onClick = { onAction(ResetPasswordAction.Submit) },
                        isLoading = state.isSubmitting,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordScreenPreview() {
    UniRideTheme {
        ResetPasswordScreen(
            state = ResetPasswordUiState(),
            onAction = {},
            onGoToLogin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordSuccessPreview() {
    UniRideTheme {
        ResetPasswordScreen(
            state = ResetPasswordUiState(isSuccess = true),
            onAction = {},
            onGoToLogin = {},
        )
    }
}
