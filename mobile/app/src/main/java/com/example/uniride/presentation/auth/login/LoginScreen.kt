package com.example.uniride.presentation.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.presentation.auth.components.InlineFeedbackBanner
import com.example.uniride.presentation.auth.components.PasswordTextField
import com.example.uniride.presentation.common.LabeledTextField
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToMain -> onLoginSuccess()
            }
        }
    }

    LoginScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onForgotPasswordClick = onForgotPasswordClick,
        onRegisterClick = onRegisterClick,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    onBack: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.auth_login_title),
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
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
            ) {
                Text(
                    text = stringResource(R.string.auth_login_subtitle),
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
                    onValueChange = { onAction(LoginAction.EmailChanged(it)) },
                    label = stringResource(R.string.auth_field_email_label),
                    placeholder = stringResource(R.string.auth_field_email_placeholder),
                    error = state.emailErrorRes?.let { stringResource(it) },
                    enabled = !state.isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false,
                    ),
                )

                PasswordTextField(
                    value = state.password,
                    onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
                    label = stringResource(R.string.auth_field_password_label),
                    error = state.passwordErrorRes?.let { stringResource(it) },
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { onAction(LoginAction.Submit) }),
                )

                TextButton(
                    onClick = onForgotPasswordClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(R.string.auth_login_forgot_password))
                }

                PrimaryButton(
                    text = stringResource(R.string.auth_login_submit),
                    onClick = { onAction(LoginAction.Submit) },
                    isLoading = state.isSubmitting,
                )

                TextButton(
                    onClick = onRegisterClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(R.string.auth_login_no_account))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    UniRideTheme {
        LoginScreen(
            state = LoginUiState(email = "lfernandez@uade.edu.ar"),
            onAction = {},
            onBack = {},
            onForgotPasswordClick = {},
            onRegisterClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    UniRideTheme {
        LoginScreen(
            state = LoginUiState(
                email = "lfernandez@gmail.com",
                emailErrorRes = R.string.auth_error_email_domain,
                isOffline = true,
            ),
            onAction = {},
            onBack = {},
            onForgotPasswordClick = {},
            onRegisterClick = {},
        )
    }
}
