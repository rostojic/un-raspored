package com.ostojic.raspored.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ostojic.raspored.R
import com.ostojic.raspored.domain.DailyMessage
import com.ostojic.raspored.domain.DayGroup
import com.ostojic.raspored.domain.ResolvedLesson
import com.ostojic.raspored.domain.Shift
import com.ostojic.raspored.presentation.DailyUiState
import com.ostojic.raspored.ui.theme.RasporedTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Stateless Daily view. Renders a [DailyUiState] and exposes navigation
 * callbacks; the hosting Activity/ViewModel owns the state.
 *
 * Rendering rules (Requirement 1, 3):
 * - Header shows the localized weekday, the group when present, and the
 *   selected date in short numeric form, e.g. "Utorak - Neparna - 15.09.'27.".
 *   (Req 1.3)
 * - A shift label is shown when a shift applies, preceded by a sun (morning) or
 *   moon (afternoon) icon. (Req 1.4)
 * - When [DailyUiState.message] is non-null the corresponding message is shown
 *   and the period list is omitted. (Req 1.6, 1.7)
 * - Otherwise the resolved lessons are rendered in the given (ascending) order,
 *   each with its period number, class code and start-end times. (Req 1.1, 1.2)
 * - Pause periods show the [R.string.pause_label] in place of a class code as a
 *   visible no-class indication. (Req 1.5)
 * - Prev/next controls call [onPreviousDay]/[onNextDay]; a calendar action calls
 *   [onOpenCalendar]. (Req 3.2, 3.3)
 * - A top app bar overflow menu (⋮) exposes "Подешавања" (Settings) and
 *   "Измени распоред" (Edit schedule), calling [onOpenSettings]/[onOpenEditor].
 *   (Req 7.2, 8.2)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    state: DailyUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.owner.isNotBlank()) {
                            stringResource(R.string.app_bar_title_owner, state.owner)
                        } else {
                            stringResource(R.string.app_bar_title)
                        }
                    )
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_menu)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_settings)) },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_edit_schedule)) },
                            onClick = {
                                menuExpanded = false
                                onOpenEditor()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            DailyHeader(state)

            Spacer(Modifier.height(8.dp))

            // Body: either a message (weekend / no classes / errors) or the period list.
            val message = state.message
            if (message != null) {
                Text(
                    text = stringResource(message.stringRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 32.dp)
                )
            } else {
                LessonList(
                    lessons = state.lessons,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            NavigationControls(
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onOpenCalendar = onOpenCalendar
            )
        }
    }
}

/**
 * Short numeric date formatter, e.g. 15.09.'27. -- day and month zero-padded,
 * two-digit year prefixed with an apostrophe. Locale-independent.
 */
private val HEADER_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yy")

/**
 * Weekday, optional group, and the selected date, plus the optional shift row
 * with its icon. (Req 1.3, 1.4)
 */
@Composable
private fun DailyHeader(state: DailyUiState) {
    Column {
        // Abbreviated Cyrillic weekday, group when present, and the short
        // numeric date, space-separated, e.g. "Чет Парна 17.09.26". (Req 1.3)
        val weekday = stringResource(state.dayOfWeek.abbrStringRes())
        val dateText = state.date.format(HEADER_DATE_FORMATTER)
        val headerText = buildString {
            append(weekday)
            state.group?.let { append(" ").append(groupText(it)) }
            append(" ").append(dateText)
        }
        Text(
            text = headerText,
            style = MaterialTheme.typography.headlineSmall
        )

        // Shift label with a sun (morning) / moon (afternoon) icon. (Req 1.4)
        state.shift?.let { shift ->
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(shift.iconRes()),
                    contentDescription = stringResource(shift.iconDescRes()),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(shift.stringRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Ordered period rows. Lessons are already ascending by period. (Req 1.1, 1.2) */
@Composable
private fun LessonList(
    lessons: List<ResolvedLesson>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(lessons, key = { it.period }) { lesson ->
            LessonRow(lesson)
        }
    }
}

/**
 * A single period row: period number, class code (or pause indication), and the
 * start-end times. (Req 1.1, 1.5)
 */
@Composable
private fun LessonRow(lesson: ResolvedLesson) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period number.
            Text(
                text = lesson.period.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            // Class code, or a visible "no class" indication for pauses. (Req 1.5)
            if (lesson.isPause) {
                Text(
                    text = stringResource(R.string.pause_label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = lesson.classCode.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            // Start-end times formatted as HH:mm.
            Text(
                text = "${formatTime(lesson.start)}\u2013${formatTime(lesson.end)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Previous / calendar / next controls. (Req 3.2, 3.3) */
@Composable
private fun NavigationControls(
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.action_previous_day)
                )
            }
            IconButton(
                onClick = onOpenCalendar,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.action_open_calendar)
                )
            }
            IconButton(
                onClick = onNextDay,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.action_next_day)
                )
            }
        }
    }
}

/** Formats a [LocalTime] as HH:mm without a locale-dependent formatter. */
private fun formatTime(time: LocalTime): String =
    "%02d:%02d".format(time.hour, time.minute)

/** Maps a [DayOfWeek] to its localized weekday string resource. (Req 1.3) */
@StringRes
private fun DayOfWeek.stringRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_monday
    DayOfWeek.TUESDAY -> R.string.day_tuesday
    DayOfWeek.WEDNESDAY -> R.string.day_wednesday
    DayOfWeek.THURSDAY -> R.string.day_thursday
    DayOfWeek.FRIDAY -> R.string.day_friday
    DayOfWeek.SATURDAY -> R.string.day_saturday
    DayOfWeek.SUNDAY -> R.string.day_sunday
}

/** Maps a [DayOfWeek] to its abbreviated Cyrillic weekday string resource. (Feature 1) */
@StringRes
private fun DayOfWeek.abbrStringRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_abbr_monday
    DayOfWeek.TUESDAY -> R.string.day_abbr_tuesday
    DayOfWeek.WEDNESDAY -> R.string.day_abbr_wednesday
    DayOfWeek.THURSDAY -> R.string.day_abbr_thursday
    DayOfWeek.FRIDAY -> R.string.day_abbr_friday
    DayOfWeek.SATURDAY -> R.string.day_abbr_saturday
    DayOfWeek.SUNDAY -> R.string.day_abbr_sunday
}

/** Resolves the localized group label for a [DayGroup]. (Req 1.3) */
@Composable
private fun groupText(group: DayGroup): String = stringResource(
    when (group) {
        DayGroup.PARNA -> R.string.group_parna
        DayGroup.NEPARNA -> R.string.group_neparna
    }
)

/** Maps a [Shift] to its localized short label. (Req 1.4) */
@StringRes
private fun Shift.stringRes(): Int = when (this) {
    Shift.MORNING -> R.string.shift_morning
    Shift.AFTERNOON -> R.string.shift_afternoon
}

/** Maps a [Shift] to its sun (morning) / moon (afternoon) icon. (Req 1.4) */
@DrawableRes
private fun Shift.iconRes(): Int = when (this) {
    Shift.MORNING -> R.drawable.ic_shift_morning
    Shift.AFTERNOON -> R.drawable.ic_shift_afternoon
}

/** Content description for the shift icon. */
@StringRes
private fun Shift.iconDescRes(): Int = when (this) {
    Shift.MORNING -> R.string.shift_morning_icon_desc
    Shift.AFTERNOON -> R.string.shift_afternoon_icon_desc
}

/** Maps a [DailyMessage] to its localized message string resource. (Req 1.6, 1.7) */
@StringRes
private fun DailyMessage.stringRes(): Int = when (this) {
    DailyMessage.WEEKEND -> R.string.message_weekend
    DailyMessage.NO_CLASSES -> R.string.message_no_classes
    DailyMessage.DATA_UNAVAILABLE -> R.string.message_data_unavailable
    DailyMessage.DATE_UNDETERMINED -> R.string.message_date_undetermined
}

@Preview(showBackground = true)
@Composable
private fun DailyScreenPreview() {
    RasporedTheme {
        Surface {
            DailyScreen(
                state = DailyUiState(
                    date = LocalDate.of(2027, 9, 15),
                    dayOfWeek = DayOfWeek.TUESDAY,
                    group = DayGroup.NEPARNA,
                    shift = Shift.AFTERNOON,
                    lessons = listOf(
                        ResolvedLesson(2, "6/1", LocalTime.of(14, 50), LocalTime.of(15, 35)),
                        ResolvedLesson(3, "8/5", LocalTime.of(15, 55), LocalTime.of(16, 40))
                    ),
                    message = null,
                    owner = "Оливера"
                ),
                onPreviousDay = {},
                onNextDay = {},
                onOpenCalendar = {},
                onOpenSettings = {},
                onOpenEditor = {}
            )
        }
    }
}