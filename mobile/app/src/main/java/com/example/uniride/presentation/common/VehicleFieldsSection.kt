package com.example.uniride.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

/** Optional vehicle form section shared between Register (A3) and Profile (D1) screens (RF01). */
@Composable
fun VehicleFieldsSection(
    form: ProfileForm,
    enabled: Boolean,
    onToggleHasVehicle: (Boolean) -> Unit,
    onBrandChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onPlateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
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
                checked = form.hasVehicle,
                onCheckedChange = onToggleHasVehicle,
                enabled = enabled,
            )
        }

        form.vehicleRequiredErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (form.hasVehicle) {
            LabeledTextField(
                value = form.vehicleBrand,
                onValueChange = onBrandChange,
                label = stringResource(R.string.auth_field_vehicle_brand_label),
                error = form.vehicleBrandErrorRes?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            LabeledTextField(
                value = form.vehicleModel,
                onValueChange = onModelChange,
                label = stringResource(R.string.auth_field_vehicle_model_label),
                error = form.vehicleModelErrorRes?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            LabeledTextField(
                value = form.vehicleColor,
                onValueChange = onColorChange,
                label = stringResource(R.string.auth_field_vehicle_color_label),
                error = form.vehicleColorErrorRes?.let { stringResource(it) },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            LabeledTextField(
                value = form.vehiclePlate,
                onValueChange = onPlateChange,
                label = stringResource(R.string.auth_field_vehicle_plate_label),
                error = form.vehiclePlateErrorRes?.let { stringResource(it) },
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

@Preview(showBackground = true)
@Composable
private fun VehicleFieldsSectionPreview() {
    UniRideTheme {
        VehicleFieldsSection(
            form = ProfileForm(
                hasVehicle = true,
                vehicleBrand = "Volkswagen",
                vehicleModel = "Gol Trend",
                vehicleColor = "Gris",
                vehiclePlate = "AB123CD",
            ),
            enabled = true,
            onToggleHasVehicle = {},
            onBrandChange = {},
            onModelChange = {},
            onColorChange = {},
            onPlateChange = {},
        )
    }
}
