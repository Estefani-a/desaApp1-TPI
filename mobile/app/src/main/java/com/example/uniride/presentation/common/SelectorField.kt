package com.example.uniride.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.domain.model.Shift
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Field to choose one value of a closed list: campus, shift, direction… (CONVENCIONES §7.4).
 *
 * @param anyLabel when not `null`, the list starts with this option, which selects `null` (for
 * filters: "Todas las sedes"). Forms that require a value leave it `null`.
 * @param optionLabel text of each option; it is composable so it can read strings.xml
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T?) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    anyLabel: String? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = when {
        selected != null -> optionLabel(selected)
        anyLabel != null -> anyLabel
        else -> ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (anyLabel != null) {
                DropdownMenuItem(
                    text = { Text(anyLabel) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectorFieldPreview() {
    UniRideTheme {
        SelectorField(
            label = "Turno",
            options = Shift.entries,
            selected = Shift.NOCHE,
            onSelected = {},
            optionLabel = { stringResource(it.labelRes) },
            anyLabel = stringResource(R.string.common_any),
        )
    }
}
