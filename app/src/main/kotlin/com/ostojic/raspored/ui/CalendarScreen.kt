package com.ostojic.raspored.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ostojic.raspored.ui.theme.RasporedTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Calendar view: a Material 3 date picker that lets the teacher jump to any
 * date's Daily view.
 *
 * The picker opens defaulting to the month containing [selectedDate] and
 * visually marks that date as the current selection (Requirement 4.1, 4.3).
 * Confirming a date invokes [onDateSelected] with the chosen [LocalDate]; the
 * caller is responsible for calling `viewModel.selectDate(date)` and navigating
 * back to the Daily view (Requirement 4.2, 4.4). Dismissing without choosing
 * invokes [onDismiss].
 *
 * This composable is deliberately stateless (it takes a date and callbacks
 * rather than the ViewModel) so it stays easy to preview and test.
 *
 * Note: weekend selections are allowed and simply passed through. The Daily
 * view rendered afterwards shows the "no classes" message and retains the
 * selected date via the ViewModel state (Requirement 4.5).
 *
 * @param selectedDate the currently selected date; the picker defaults to its
 *   month and marks it as selected.
 * @param onDateSelected invoked with the confirmed date. The caller wires this
 *   to `viewModel.selectDate` and navigation back to the Daily view.
 * @param onDismiss invoked when the teacher closes the picker without choosing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Convert the selected date to UTC epoch millis so the picker both defaults
    // to its month and marks it as selected. (Requirement 4.1, 4.3)
    val selectedMillis = remember(selectedDate) { selectedDate.toUtcEpochMilli() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        initialDisplayedMonthMillis = selectedMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    // Convert the picked millis back to a LocalDate and hand it
                    // to the caller. (Requirement 4.2, 4.4)
                    val picked = datePickerState.selectedDateMillis?.utcMillisToLocalDate()
                        ?: selectedDate
                    onDateSelected(picked)
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        modifier = modifier
    ) {
        DatePicker(state = datePickerState)
    }
}

/** Converts a [LocalDate] to UTC epoch milliseconds at start of day. */
private fun LocalDate.toUtcEpochMilli(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Converts UTC epoch milliseconds back to a [LocalDate]. */
private fun Long.utcMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    RasporedTheme {
        CalendarScreen(
            selectedDate = LocalDate.of(2026, 8, 31),
            onDateSelected = {},
            onDismiss = {}
        )
    }
}
