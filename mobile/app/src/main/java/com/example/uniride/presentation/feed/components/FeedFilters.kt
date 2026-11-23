package com.example.uniride.presentation.feed.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.model.Shift
import com.example.uniride.domain.model.TripFilters
import com.example.uniride.presentation.common.SpanishArgentina
import com.example.uniride.presentation.common.dateLabel
import com.example.uniride.presentation.common.labelRes
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import java.time.LocalDate

/**
 * Filters of B1 (RF03) as in the guide: the direction as a two-option pill ("Vuelta a casa" /
 * "Ida a la sede"; touching the selected one again shows both) and a row of chips for campus,
 * date and shift, each with a menu that starts with "any". Every change goes out at once
 * ([onChange]): the list reacts without a "Buscar" button.
 */
@Composable
fun FeedFilters(
    filters: TripFilters,
    campuses: List<Campus>,
    dateOptions: List<LocalDate>,
    onChange: (TripFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        DirectionToggle(
            selected = filters.direction,
            onSelect = { onChange(filters.copy(direction = it)) },
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        FilterChipsRow(filters, campuses, dateOptions, onChange)
    }
}

@Composable
private fun DirectionToggle(
    selected: Direction?,
    onSelect: (Direction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        Direction.VUELTA to R.string.feed_direction_vuelta,
        Direction.IDA to R.string.feed_direction_ida,
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.selectableGroup()) {
            options.forEach { (direction, labelRes) ->
                val isSelected = direction == selected
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(if (isSelected) null else direction) },
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    filters: TripFilters,
    campuses: List<Campus>,
    dateOptions: List<LocalDate>,
    onChange: (TripFilters) -> Unit,
) {
    val campusNames = campuses.associate { it.code to it.name }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The chips already say what they filter: the icon is decorative
        Icon(
            painter = painterResource(R.drawable.ic_tune),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.IconMedium),
        )
        DropdownChip(
            label = filters.campus?.let { campusNames[it] ?: it.value }
                ?: stringResource(R.string.feed_filter_campus),
            selected = filters.campus != null,
            anyLabel = stringResource(R.string.feed_filter_any_campus),
            options = campuses.map { it.code },
            optionLabel = { code: CampusCode -> campusNames[code] ?: code.value },
            onSelected = { onChange(filters.copy(campus = it)) },
        )
        DropdownChip(
            label = filters.date?.let { dateLabel(it) } ?: stringResource(R.string.feed_filter_date),
            selected = filters.date != null,
            anyLabel = stringResource(R.string.feed_filter_any_date),
            options = dateOptions,
            optionLabel = { dateLabel(it) },
            onSelected = { onChange(filters.copy(date = it)) },
        )
        DropdownChip(
            label = filters.shift?.let {
                stringResource(R.string.feed_filter_shift_selected, stringResource(it.labelRes).lowercase(SpanishArgentina))
            } ?: stringResource(R.string.feed_filter_shift),
            selected = filters.shift != null,
            anyLabel = stringResource(R.string.feed_filter_any_shift),
            options = Shift.entries,
            optionLabel = { stringResource(it.labelRes) },
            onSelected = { onChange(filters.copy(shift = it)) },
        )
    }
}

/** Chip that opens a menu: the first option ([anyLabel]) clears the filter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownChip(
    label: String,
    selected: Boolean,
    anyLabel: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(anyLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedFiltersPreview() {
    UniRideTheme {
        FeedFilters(
            filters = TripFilters(campus = CampusCode("MONSERRAT"), direction = Direction.VUELTA, shift = Shift.NOCHE),
            campuses = listOf(Campus(CampusCode("MONSERRAT"), "Sede Monserrat", emptyList())),
            dateOptions = listOf(LocalDate.now()),
            onChange = {},
        )
    }
}
