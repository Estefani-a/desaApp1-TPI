package com.example.uniride.presentation.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.presentation.auth.components.InlineFeedbackBanner
import com.example.uniride.presentation.auth.components.PasswordTextField
import com.example.uniride.presentation.auth.components.VehicleFieldsSection
import com.example.uniride.presentation.auth.components.labelRes
import com.example.uniride.presentation.common.LabeledTextField
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.SelectorField
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

@Composable
fun RegisterRoute(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onRegistered: (email: String) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RegisterEffect.NavigateToCheckEmail -> onRegistered(effect.email)
            }
        }
    }

    RegisterScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onLoginClick = onLoginClick,
    )
}

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isSubmitting

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.auth_register_title),
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
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                Text(
                    text = stringResource(R.string.auth_register_subtitle),
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
                    onValueChange = { onAction(RegisterAction.EmailChanged(it)) },
                    label = stringResource(R.string.auth_field_email_label),
                    placeholder = stringResource(R.string.auth_field_email_placeholder),
                    supportingText = stringResource(R.string.auth_field_email_supporting),
                    error = state.emailErrorRes?.let { stringResource(it) },
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false,
                    ),
                )

                PasswordTextField(
                    value = state.password,
                    onValueChange = { onAction(RegisterAction.PasswordChanged(it)) },
                    label = stringResource(R.string.auth_field_password_label),
                    supportingText = stringResource(R.string.auth_field_password_supporting),
                    error = state.passwordErrorRes?.let { stringResource(it) },
                    enabled = enabled,
                    imeAction = ImeAction.Next,
                )

                PasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = { onAction(RegisterAction.ConfirmPasswordChanged(it)) },
                    label = stringResource(R.string.auth_field_confirm_password_label),
                    error = state.confirmPasswordErrorRes?.let { stringResource(it) },
                    enabled = enabled,
                    imeAction = ImeAction.Next,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpaceS))

                LabeledTextField(
                    value = state.fullName,
                    onValueChange = { onAction(RegisterAction.FullNameChanged(it)) },
                    label = stringResource(R.string.auth_field_full_name_label),
                    error = state.fullNameErrorRes?.let { stringResource(it) },
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )

                LabeledTextField(
                    value = state.career,
                    onValueChange = { onAction(RegisterAction.CareerChanged(it)) },
                    label = stringResource(R.string.auth_field_career_label),
                    error = state.careerErrorRes?.let { stringResource(it) },
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )

                LabeledTextField(
                    value = state.phone,
                    onValueChange = { onAction(RegisterAction.PhoneChanged(it)) },
                    label = stringResource(R.string.auth_field_phone_label),
                    supportingText = stringResource(R.string.auth_field_phone_supporting),
                    error = state.phoneErrorRes?.let { stringResource(it) },
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                )

                SelectorField(
                    label = stringResource(R.string.auth_field_preferred_mode_label),
                    options = PreferredMode.entries,
                    selected = state.preferredMode,
                    onSelected = { mode ->
                        if (mode != null) onAction(RegisterAction.PreferredModeChanged(mode))
                    },
                    optionLabel = { stringResource(it.labelRes()) },
                    enabled = enabled,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpaceS))

                VehicleFieldsSection(
                    hasVehicle = state.hasVehicle,
                    brand = state.vehicleBrand,
                    model = state.vehicleModel,
                    color = state.vehicleColor,
                    plate = state.vehiclePlate,
                    brandErrorRes = state.vehicleBrandErrorRes,
                    modelErrorRes = state.vehicleModelErrorRes,
                    colorErrorRes = state.vehicleColorErrorRes,
                    plateErrorRes = state.vehiclePlateErrorRes,
                    enabled = enabled,
                    onToggleHasVehicle = { onAction(RegisterAction.HasVehicleChanged(it)) },
                    onBrandChange = { onAction(RegisterAction.VehicleBrandChanged(it)) },
                    onModelChange = { onAction(RegisterAction.VehicleModelChanged(it)) },
                    onColorChange = { onAction(RegisterAction.VehicleColorChanged(it)) },
                    onPlateChange = { onAction(RegisterAction.VehiclePlateChanged(it)) },
                )

                PrimaryButton(
                    text = stringResource(R.string.auth_register_submit),
                    onClick = { onAction(RegisterAction.Submit) },
                    isLoading = state.isSubmitting,
                    modifier = Modifier.padding(top = Dimens.SpaceS),
                )

                TextButton(
                    onClick = onLoginClick,
                    enabled = enabled,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(R.string.auth_register_has_account))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    UniRideTheme {
        RegisterScreen(
            state = RegisterUiState(
                email = "jperez@uade.edu.ar",
                fullName = "Juan Pérez",
                career = "Ingeniería en Informática",
                preferredMode = PreferredMode.AMBOS,
                hasVehicle = true,
            ),
            onAction = {},
            onBack = {},
            onLoginClick = {},
        )
    }
}
