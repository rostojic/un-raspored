package com.ostojic.raspored.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ostojic.raspored.R
import com.ostojic.raspored.domain.DayGroup
import com.ostojic.raspored.domain.Lesson
import com.ostojic.raspored.domain.StoredSchedule
import com.ostojic.raspored.presentation.ClassCodeError
import com.ostojic.raspored.presentation.EditorNotice
import com.ostojic.raspored.presentation.EditorUiState
import com.ostojic.raspored.ui.theme.RasporedTheme
import java.time.DayOfWeek

/**
 * Stateless Schedule Editor screen. Renders an [EditorUiState] and exposes edit
 * and action callbacks; the hosting ViewModel owns the working copy.
 *
 * For each weekday Monday–Friday it renders a section with a ПАРНА/НЕПАРНА group
 * toggle (Req 8.3, 8.6) followed by seven editable period rows for periods 1..7,
 * each an [OutlinedTextField] bound to the current class code (empty for a Pause,
 * Req 8.2, 8.5). A cell flagged [ClassCodeError.TOO_LONG] shows an error and its
 * supporting text (Req 8.4). Save persists the working copy (Req 8.8) and Revert
 * restores the previous schedule (Req 9.3); Revert is disabled when there is no
 * previous schedule (Req 9.6). A save error (Req 8.9) is shown as error text and
 * one-shot notices (SAVED/REVERTED/NO_PREVIOUS) are shown via a Snackbar. Only
 * the working copy is edited until Save (Req 8.7).
 *
 * @param onConsumeNotice invoked after a [EditorNotice] has been shown so the
 *   ViewModel can clear it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    state: EditorUiState,
    onClassCodeChanged: (DayOfWeek, Int, String) -> Unit,
    onGroupToggled: (DayOfWeek, DayGroup) -> Unit,
    onSave: () -> Unit,
    onRevert: () -> Unit,
    onBack: () -> Unit,
    onConsumeNotice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve the one-shot notice message in composable scope so it can be used
    // inside the (non-composable) LaunchedEffect lambda below.
    val noticeMessage = state.notice?.let { noticeText(it) }

    // Show one-shot notices (saved / reverted / no previous) then clear them.
    LaunchedEffect(state.notice) {
        if (noticeMessage == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(noticeMessage)
        onConsumeNotice()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (day in EDITABLE_DAYS) {
                item(key = "header-$day") {
                    DaySection(
                        day = day,
                        state = state,
                        onClassCodeChanged = onClassCodeChanged,
                        onGroupToggled = onGroupToggled
                    )
                }
            }

            // Save error indication (Req 8.9).
            state.saveError?.let { error ->
                item(key = "save-error") {
                    Text(
                        text = stringResource(R.string.save_failed) + ": $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }

            item(key = "actions") {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRevert,
                        enabled = state.hasPrevious,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_revert))
                    }
                }
            }
        }
    }
}

/**
 * One weekday's editor block: a header, a ПАРНА/НЕПАРНА group toggle, and seven
 * period rows.
 */
@Composable
private fun DaySection(
    day: DayOfWeek,
    state: EditorUiState,
    onClassCodeChanged: (DayOfWeek, Int, String) -> Unit,
    onGroupToggled: (DayOfWeek, DayGroup) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = stringResource(dayNameRes(day)),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(4.dp))

        GroupToggle(
            selected = state.working.groupByDay[day],
            onSelect = { group -> onGroupToggled(day, group) }
        )
        Spacer(Modifier.height(8.dp))

        for (period in PERIODS) {
            PeriodRow(
                period = period,
                classCode = classCodeOf(state.working, day, period),
                isError = state.classCodeErrors[day to period] != null,
                onValueChange = { text -> onClassCodeChanged(day, period, text) }
            )
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

/** ПАРНА / НЕПАРНА group selector for a weekday (Req 8.3, 8.6). */
@Composable
private fun GroupToggle(
    selected: DayGroup?,
    onSelect: (DayGroup) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == DayGroup.PARNA,
            onClick = { onSelect(DayGroup.PARNA) },
            label = { Text(stringResource(R.string.group_parna_upper)) }
        )
        FilterChip(
            selected = selected == DayGroup.NEPARNA,
            onClick = { onSelect(DayGroup.NEPARNA) },
            label = { Text(stringResource(R.string.group_neparna_upper)) }
        )
    }
}

/** A single editable period row: the period number and its class-code field. */
@Composable
private fun PeriodRow(
    period: Int,
    classCode: String,
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = period.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = classCode,
            onValueChange = onValueChange,
            singleLine = true,
            isError = isError,
            label = { Text(stringResource(R.string.editor_class_code_label)) },
            supportingText = if (isError) {
                { Text(stringResource(R.string.class_code_error_too_long)) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The current class-code text for ([day], [period]); empty string for a Pause. */
private fun classCodeOf(schedule: StoredSchedule, day: DayOfWeek, period: Int): String =
    schedule.lessonsByDay[day]?.firstOrNull { it.period == period }?.classCode ?: ""

/** Maps a weekday to its full Serbian Cyrillic name string resource. */
@StringRes
private fun dayNameRes(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.day_cyr_monday
    DayOfWeek.TUESDAY -> R.string.day_cyr_tuesday
    DayOfWeek.WEDNESDAY -> R.string.day_cyr_wednesday
    DayOfWeek.THURSDAY -> R.string.day_cyr_thursday
    DayOfWeek.FRIDAY -> R.string.day_cyr_friday
    DayOfWeek.SATURDAY -> R.string.day_cyr_saturday
    DayOfWeek.SUNDAY -> R.string.day_cyr_sunday
}

/** One-shot notice text resolved from string resources (composable scope only). */
@Composable
private fun noticeText(notice: EditorNotice): String = stringResource(
    when (notice) {
        EditorNotice.SAVED -> R.string.notice_saved
        EditorNotice.REVERTED -> R.string.notice_reverted
        EditorNotice.NO_PREVIOUS -> R.string.notice_no_previous
    }
)

/** The weekdays the editor exposes, in display order. */
private val EDITABLE_DAYS: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
)

/** The period numbers exposed per weekday. */
private val PERIODS: IntRange = 1..7

@Preview(showBackground = true)
@Composable
private fun ScheduleEditorScreenPreview() {
    val sampleDays = EDITABLE_DAYS.associateWith { day ->
        PERIODS.map { period ->
            Lesson(period = period, classCode = if (period <= 2) "5/$period" else null)
        }
    }
    RasporedTheme {
        ScheduleEditorScreen(
            state = EditorUiState(
                working = StoredSchedule(
                    lessonsByDay = sampleDays,
                    groupByDay = mapOf(
                        DayOfWeek.MONDAY to DayGroup.PARNA,
                        DayOfWeek.TUESDAY to DayGroup.NEPARNA,
                        DayOfWeek.WEDNESDAY to DayGroup.NEPARNA,
                        DayOfWeek.THURSDAY to DayGroup.PARNA,
                        DayOfWeek.FRIDAY to DayGroup.PARNA
                    )
                ),
                hasPrevious = true,
                classCodeErrors = emptyMap(),
                saveError = null,
                notice = null
            ),
            onClassCodeChanged = { _, _, _ -> },
            onGroupToggled = { _, _ -> },
            onSave = {},
            onRevert = {},
            onBack = {}
        )
    }
}
