package com.example.uniride.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.uniride.domain.model.Rating
import com.example.uniride.presentation.auth.components.InlineFeedbackBanner
import com.example.uniride.presentation.auth.components.VehicleFieldsSection
import com.example.uniride.presentation.auth.components.labelRes
import com.example.uniride.presentation.common.ErrorState
import com.example.uniride.presentation.common.LabeledTextField
import com.example.uniride.presentation.common.LoadingState
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.SelectorField
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.common.formatRating
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

@Composable
fun ProfileScreenRoute(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToAuth -> onLoggedOut()
            }
        }
    }

    ProfileScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onAction(ProfileAction.DismissLogoutDialog) },
            title = { Text(stringResource(R.string.profile_logout_dialog_title)) },
            text = { Text(stringResource(R.string.profile_logout_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(ProfileAction.ConfirmLogout) }) {
                    Text(
                        text = stringResource(R.string.profile_logout_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ProfileAction.DismissLogoutDialog) }) {
                    Text(stringResource(R.string.profile_logout_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.profile_title),
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isOffline) {
                OfflineBanner(lastUpdatedAt = null)
            }

            when {
                state.isLoading && !state.hasLoadedUser -> LoadingState()
                !state.hasLoadedUser && state.loadErrorRes != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ErrorState(
                            title = stringResource(R.string.profile_empty_title),
                            message = stringResource(state.loadErrorRes),
                            onRetry = { onAction(ProfileAction.RetryLoad) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { onAction(ProfileAction.RequestLogout) },
                            enabled = !state.isLoggingOut,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.ScreenPadding)
                                .heightIn(min = Dimens.MinTouchTarget),
                        ) {
                            Text(stringResource(R.string.profile_logout_action))
                        }
                    }
                }
                else -> ProfileFormContent(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ProfileFormContent(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
) {
    val enabled = !state.isSaving && !state.isLoggingOut

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(Dimens.SpaceL),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                Text(
                    text = stringResource(R.string.profile_verified_email_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val ratingText = state.rating?.let { rating ->
                    stringResource(
                        R.string.profile_rating_label,
                        formatRating(rating.average),
                        rating.count,
                    )
                } ?: stringResource(R.string.profile_rating_empty)
                Text(
                    text = ratingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.feedbackMessageRes?.let { messageRes ->
            InlineFeedbackBanner(
                message = stringResource(messageRes),
                isError = state.isFeedbackError,
            )
        }

        LabeledTextField(
            value = state.fullName,
            onValueChange = { onAction(ProfileAction.FullNameChanged(it)) },
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
            onValueChange = { onAction(ProfileAction.CareerChanged(it)) },
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
            onValueChange = { onAction(ProfileAction.PhoneChanged(it)) },
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
                if (mode != null) onAction(ProfileAction.PreferredModeChanged(mode))
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
            onToggleHasVehicle = { onAction(ProfileAction.HasVehicleChanged(it)) },
            onBrandChange = { onAction(ProfileAction.VehicleBrandChanged(it)) },
            onModelChange = { onAction(ProfileAction.VehicleModelChanged(it)) },
            onColorChange = { onAction(ProfileAction.VehicleColorChanged(it)) },
            onPlateChange = { onAction(ProfileAction.VehiclePlateChanged(it)) },
        )

        PrimaryButton(
            text = stringResource(R.string.profile_save_action),
            onClick = { onAction(ProfileAction.Save) },
            isLoading = state.isSaving,
            enabled = !state.isLoggingOut,
            modifier = Modifier.padding(top = Dimens.SpaceS),
        )

        OutlinedButton(
            onClick = { onAction(ProfileAction.RequestLogout) },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(stringResource(R.string.profile_logout_action))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    UniRideTheme {
        ProfileScreen(
            state = ProfileUiState(
                hasLoadedUser = true,
                isLoading = false,
                email = "lfernandez@uade.edu.ar",
                fullName = "Lucía Fernández",
                career = "Ingeniería en Informática",
                phone = "+5491155550001",
                preferredMode = PreferredMode.AMBOS,
                hasVehicle = true,
                vehicleBrand = "Volkswagen",
                vehicleModel = "Gol Trend",
                vehicleColor = "Gris",
                vehiclePlate = "AB123CD",
                rating = Rating(4.8, 12),
            ),
            onAction = {},
        )
    }
}
