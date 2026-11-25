package com.example.uniride.presentation.publish.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import com.example.uniride.R
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.Direction
import com.example.uniride.domain.validation.TripDraftRule
import com.example.uniride.presentation.common.BusinessZone
import com.example.uniride.presentation.common.LabeledTextField
import com.example.uniride.presentation.common.SelectorField
import com.example.uniride.presentation.common.dateLabel
import com.example.uniride.presentation.common.formatTime
import com.example.uniride.presentation.common.labelRes
import com.example.uniride.presentation.publish.PublishField
import com.example.uniride.presentation.publish.PublishTripAction
import com.example.uniride.presentation.publish.PublishTripUiState
import com.example.uniride.presentation.theme.Dimens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * The fields of C2, in the order the driver thinks about the trip. It is only drawing: what is
 * valid and what is shown as an error comes in [state]. While [PublishTripUiState.isSubmitting]
 * the fields are locked.
 */
@Composable
fun PublishTripForm(
    state: PublishTripUiState,
    campuses: List<Campus>,
    onAction: (PublishTripAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val enabled = !state.isSubmitting

    @Composable
    fun errorOf(field: PublishField): String? = state.errors[field]?.let { stringResource(it) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)) {
        SelectorField(
            label = stringResource(R.string.publish_campus),
            options = campuses,
            selected = campuses.firstOrNull { it.code == form.campus },
            onSelected = { campus -> campus?.let { onAction(PublishTripAction.SelectCampus(it.code)) } },
            optionLabel = { it.name },
            error = errorOf(PublishField.CAMPUS),
            enabled = enabled,
        )

        DirectionSelector(
            selected = form.direction,
            onSelected = { onAction(PublishTripAction.SelectDirection(it)) },
            enabled = enabled,
        )

        SelectorField(
            label = stringResource(R.string.publish_meeting_point),
            options = state.meetingPoints,
            selected = form.meetingPoint,
            onSelected = { point -> point?.let { onAction(PublishTripAction.SelectMeetingPoint(it)) } },
            optionLabel = { it },
            error = errorOf(PublishField.MEETING_POINT),
            enabled = enabled && form.campus != null,
        )

        LabeledTextField(
            value = form.neighborhood,
            onValueChange = { onAction(PublishTripAction.ChangeNeighborhood(it)) },
            label = stringResource(R.string.publish_neighborhood),
            error = errorOf(PublishField.NEIGHBORHOOD),
            enabled = enabled,
            modifier = Modifier.onFieldLeft { onAction(PublishTripAction.FieldLeft(PublishField.NEIGHBORHOOD)) },
        )

        LabeledTextField(
            value = form.reference,
            onValueChange = { onAction(PublishTripAction.ChangeReference(it)) },
            label = stringResource(R.string.publish_reference),
            enabled = enabled,
        )

        DepartureFields(
            date = form.departureDate,
            time = form.departureTime,
            error = errorOf(PublishField.DEPARTURE),
            enabled = enabled,
            onDateChange = { onAction(PublishTripAction.ChangeDepartureDate(it)) },
            onTimeChange = { onAction(PublishTripAction.ChangeDepartureTime(it)) },
            onLeft = { onAction(PublishTripAction.FieldLeft(PublishField.DEPARTURE)) },
        )

        ArrivalField(
            time = form.arrivalTime,
            enabled = enabled,
            onChange = { onAction(PublishTripAction.ChangeArrivalTime(it)) },
        )

        SelectorField(
            label = stringResource(R.string.publish_seats),
            options = (TripDraftRule.MIN_SEATS..TripDraftRule.MAX_SEATS).toList(),
            selected = form.seats,
            onSelected = { seats -> seats?.let { onAction(PublishTripAction.ChangeSeats(it)) } },
            optionLabel = { it.toString() },
            error = errorOf(PublishField.SEATS),
            enabled = enabled,
        )

        LabeledTextField(
            value = form.contribution,
            onValueChange = { onAction(PublishTripAction.ChangeContribution(it)) },
            label = stringResource(R.string.publish_contribution),
            error = errorOf(PublishField.CONTRIBUTION),
            supportingText = stringResource(R.string.publish_contribution_help),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.onFieldLeft { onAction(PublishTripAction.FieldLeft(PublishField.CONTRIBUTION)) },
        )
    }
}

/** Calls [onLeft] when a field that had the focus loses it: the moment its error is shown. */
@Composable
private fun Modifier.onFieldLeft(onLeft: () -> Unit): Modifier {
    var hadFocus by remember { mutableStateOf(false) }
    return onFocusChanged { focus ->
        if (hadFocus && !focus.isFocused) onLeft()
        hadFocus = focus.isFocused
    }
}

@Composable
private fun DirectionSelector(
    selected: Direction,
    onSelected: (Direction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = stringResource(R.string.publish_direction),
            style = MaterialTheme.typography.labelLarge,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Direction.entries.forEachIndexed { index, direction ->
                SegmentedButton(
                    selected = direction == selected,
                    onClick = { onSelected(direction) },
                    shape = SegmentedButtonDefaults.itemShape(index, Direction.entries.size),
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(direction.labelRes))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartureFields(
    date: LocalDate?,
    time: LocalTime?,
    error: String?,
    enabled: Boolean,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onLeft: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            PickerField(
                value = date?.let { dateLabel(it) }.orEmpty(),
                label = stringResource(R.string.publish_departure_date),
                onClick = { showDatePicker = true },
                isError = error != null,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            PickerField(
                value = time?.let(::formatTime).orEmpty(),
                label = stringResource(R.string.publish_departure_time),
                onClick = { showTimePicker = true },
                isError = error != null,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showDatePicker) {
        val today = LocalDate.now(BusinessZone)
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: today).toUtcMillis(),
            selectableDates = object : SelectableDates {
                // Days are compared in UTC, the zone the picker works in
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                onLeft()
            },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChange(it.toUtcDate()) }
                    showDatePicker = false
                    onLeft()
                }) { Text(stringResource(R.string.publish_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    onLeft()
                }) { Text(stringResource(R.string.publish_picker_dismiss)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initial = time ?: DEFAULT_PICKER_TIME,
            onConfirm = {
                onTimeChange(it)
                showTimePicker = false
                onLeft()
            },
            onDismiss = {
                showTimePicker = false
                onLeft()
            },
        )
    }
}

@Composable
private fun ArrivalField(
    time: LocalTime?,
    enabled: Boolean,
    onChange: (LocalTime?) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.End) {
        PickerField(
            value = time?.let(::formatTime).orEmpty(),
            label = stringResource(R.string.publish_arrival_time),
            onClick = { showPicker = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (time != null) {
            TextButton(
                onClick = { onChange(null) },
                enabled = enabled,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(R.string.publish_arrival_clear))
            }
        }
    }

    if (showPicker) {
        TimePickerDialog(
            initial = time ?: DEFAULT_PICKER_TIME,
            onConfirm = {
                onChange(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * Read-only field that opens a picker when touched. A transparent layer on top takes the tap,
 * so TalkBack announces it as a button with its label.
 */
@Composable
private fun PickerField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            isError = isError,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.publish_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.publish_picker_dismiss)) }
        },
        text = { TimePicker(state = state) },
    )
}

private val DEFAULT_PICKER_TIME: LocalTime = LocalTime.of(18, 0)

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
