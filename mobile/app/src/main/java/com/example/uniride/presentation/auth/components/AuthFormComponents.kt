package com.example.uniride.presentation.auth.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.uniride.R
import com.example.uniride.domain.model.PreferredMode
import com.example.uniride.domain.validation.EmailValidationError
import com.example.uniride.domain.validation.PasswordValidationError
import com.example.uniride.domain.validation.ProfileValidationError
import com.example.uniride.presentation.common.LabeledTextField
import com.example.uniride.presentation.theme.Dimens

@StringRes
fun EmailValidationError.messageRes(): Int = when (this) {
    EmailValidationError.BLANK -> R.string.auth_error_email_blank
    EmailValidationError.INVALID_FORMAT -> R.string.auth_error_email_format
    EmailValidationError.INVALID_DOMAIN -> R.string.auth_error_email_domain
}

@StringRes
fun PasswordValidationError.messageRes(): Int = when (this) {
    PasswordValidationError.BLANK -> R.string.auth_error_password_blank
    PasswordValidationError.TOO_SHORT -> R.string.auth_error_password_short
    PasswordValidationError.TOO_LONG -> R.string.auth_error_password_long
    PasswordValidationError.MISSING_LETTER -> R.string.auth_error_password_letter
    PasswordValidationError.MISSING_DIGIT -> R.string.auth_error_password_digit
}

@StringRes
fun ProfileValidationError.messageRes(): Int = when (this) {
    ProfileValidationError.FULL_NAME_BLANK -> R.string.auth_error_full_name_blank
    ProfileValidationError.CAREER_BLANK -> R.string.auth_error_career_blank
    ProfileValidationError.PHONE_BLANK -> R.string.auth_error_phone_blank
    ProfileValidationError.PHONE_INVALID -> R.string.auth_error_phone_invalid
    ProfileValidationError.VEHICLE_REQUIRED -> R.string.auth_error_vehicle_required
    ProfileValidationError.VEHICLE_BRAND_BLANK -> R.string.auth_error_vehicle_brand_blank
    ProfileValidationError.VEHICLE_MODEL_BLANK -> R.string.auth_error_vehicle_model_blank
    ProfileValidationError.VEHICLE_COLOR_BLANK -> R.string.auth_error_vehicle_color_blank
    ProfileValidationError.VEHICLE_PLATE_BLANK -> R.string.auth_error_vehicle_plate_blank
    ProfileValidationError.VEHICLE_PLATE_INVALID -> R.string.auth_error_vehicle_plate_invalid
}

@StringRes
fun Set<ProfileValidationError>.firstMessageRes(vararg candidates: ProfileValidationError): Int? =
    candidates.firstOrNull { it in this }?.messageRes()

@StringRes
fun PreferredMode.labelRes(): Int = when (this) {
    PreferredMode.PASAJERO -> R.string.auth_preferred_mode_passenger
    PreferredMode.CONDUCTOR -> R.string.auth_preferred_mode_driver
    PreferredMode.AMBOS -> R.string.auth_preferred_mode_both
}

/** Password field with visibility toggle and accessible contentDescription (RF01, RNF01). */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val bottomText = error ?: supportingText

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = bottomText?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = keyboardActions,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    painter = painterResource(
                        if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                    ),
                    contentDescription = stringResource(
                        if (visible) R.string.auth_password_hide else R.string.auth_password_show,
                    ),
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

/** Inline error or confirmation banner inside forms (TalkBack polite live region). */
@Composable
fun InlineFeedbackBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Dimens.SpaceM),
        )
    }
}

/** Optional vehicle form section shared between Register and Profile screens (RF01). */
@Composable
fun VehicleFieldsSection(
    hasVehicle: Boolean,
    brand: String,
    model: String,
    color: String,
    plate: String,
    @StringRes brandErrorRes: Int?,
    @StringRes modelErrorRes: Int?,
    @StringRes colorErrorRes: Int?,
    @StringRes plateErrorRes: Int?,
    enabled: Boolean,
    onToggleHasVehicle: (Boolean) -> Unit,
    onBrandChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onPlateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes vehicleErrorRes: Int? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Dimens.SpaceM),
            ) {
                Text(
                    text = stringResource(R.string.auth_vehicle_section_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.auth_vehicle_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = hasVehicle,
                onCheckedChange = onToggleHasVehicle,
                enabled = enabled,
            )
        }

        vehicleErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (hasVehicle) {
            LabeledTextField(
                value = brand,
                onValueChange = onBrandChange,
                label = stringResource(R.string.auth_field_vehicle_brand_label),
                error = brandErrorRes?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            LabeledTextField(
                value = model,
                onValueChange = onModelChange,
                label = stringResource(R.string.auth_field_vehicle_model_label),
                error = modelErrorRes?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            LabeledTextField(
                value = color,
                onValueChange = onColorChange,
                label = stringResource(R.string.auth_field_vehicle_color_label),
                error = colorErrorRes?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            LabeledTextField(
                value = plate,
                onValueChange = onPlateChange,
                label = stringResource(R.string.auth_field_vehicle_plate_label),
                error = plateErrorRes?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                ),
            )
        }
    }
}
