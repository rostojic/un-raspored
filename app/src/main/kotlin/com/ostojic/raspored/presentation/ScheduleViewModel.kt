package com.ostojic.raspored.presentation

import androidx.lifecycle.ViewModel
import com.ostojic.raspored.domain.DailyMessage
import com.ostojic.raspored.domain.DaySchedule
import com.ostojic.raspored.domain.ScheduleService
import com.ostojic.raspored.domain.TimetableLoadResult
import com.ostojic.raspored.domain.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Holds the selected date and exposes the Daily view state.
 *
 * The [clock] is injected so that "today" is deterministic in tests; the
 * default is the system clock for production use. The [repository] is injected
 * alongside the [scheduleService] purely so this ViewModel can run the startup
 * self-check ([TimetableRepository.load]) and surface
 * [DailyMessage.DATA_UNAVAILABLE] (Req 5.7) without threading that outcome
 * through the service.
 *
 * State is computed synchronously on the calling thread on every date change,
 * so tests can read [uiState]`.value` immediately after invoking a navigation
 * method. (Requirement 3, 4, 6.2, 6.4)
 */
class ScheduleViewModel(
    private val scheduleService: ScheduleService,
    private val repository: TimetableRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    /** The date currently displayed in the Daily view. */
    private var selectedDate: LocalDate = LocalDate.now(clock)

    /** Set once [initToToday] fails to read the clock, forcing DATE_UNDETERMINED. */
    private var dateUndetermined: Boolean = false

    private val _uiState = MutableStateFlow(mapToUiState(selectedDate))

    /** Current Daily view state. */
    val uiState: StateFlow<DailyUiState> = _uiState.asStateFlow()

    /**
     * Sets the selected date to the current system date and renders it.
     * (Requirement 3.1, 6.2)
     *
     * If reading `LocalDate.now(clock)` throws (clock failure), the view falls
     * back to the most recent weekday (Mon–Fri) on/before a best-effort
     * last-known date and the resulting state carries
     * [DailyMessage.DATE_UNDETERMINED]. (Requirement 6.4)
     */
    fun initToToday() {
        try {
            dateUndetermined = false
            selectedDate = LocalDate.now(clock)
        } catch (e: Exception) {
            dateUndetermined = true
            // The injected clock failed; fall back to a best-effort "now" from
            // the system default clock. If that also fails we cannot determine
            // a date at all, so use the epoch as a last resort. Either way we
            // snap to the most recent weekday so the view shows a real schedule.
            val lastKnown = try {
                LocalDate.now()
            } catch (ignored: Exception) {
                LocalDate.ofEpochDay(0)
            }
            selectedDate = mostRecentWeekday(lastKnown)
        }
        refresh()
    }

    /** Advances the selected date by one day and re-renders. (Requirement 3.2) */
    fun nextDay() {
        dateUndetermined = false
        selectedDate = selectedDate.plusDays(1)
        refresh()
    }

    /** Moves the selected date back one day and re-renders. (Requirement 3.3, 3.4) */
    fun previousDay() {
        dateUndetermined = false
        selectedDate = selectedDate.minusDays(1)
        refresh()
    }

    /** Sets the selected date to [date] and re-renders. (Requirement 4.2) */
    fun selectDate(date: LocalDate) {
        dateUndetermined = false
        selectedDate = date
        refresh()
    }

    /** Recomputes and publishes the state for the current [selectedDate]. */
    private fun refresh() {
        _uiState.value = mapToUiState(selectedDate)
    }

    /**
     * Maps [date] to a [DailyUiState].
     *
     * Precedence of messages:
     * 1. [DailyMessage.DATA_UNAVAILABLE] when the startup self-check fails (Req 5.7).
     * 2. [DailyMessage.DATE_UNDETERMINED] when the clock could not be read (Req 6.4).
     * 3. [DailyMessage.WEEKEND] for Saturday/Sunday (Req 1.6, 3.5, 6.3).
     * 4. [DailyMessage.NO_CLASSES] for a weekday with no periods (Req 1.7).
     * Otherwise the resolved period list is shown with no message.
     */
    private fun mapToUiState(date: LocalDate): DailyUiState {
        // Req 5.7: data-unavailable takes precedence over every other message.
        if (repository.load() is TimetableLoadResult.Unavailable) {
            return DailyUiState(
                date = date,
                dayOfWeek = date.dayOfWeek,
                group = null,
                shift = null,
                lessons = emptyList(),
                message = DailyMessage.DATA_UNAVAILABLE
            )
        }

        val schedule: DaySchedule = scheduleService.scheduleFor(date)

        // Req 6.4: preserve the undetermined-date indication set by initToToday.
        if (dateUndetermined) {
            return DailyUiState(
                date = date,
                dayOfWeek = schedule.dayOfWeek,
                group = schedule.group,
                shift = schedule.shift,
                lessons = schedule.lessons,
                message = DailyMessage.DATE_UNDETERMINED
            )
        }

        // Req 1.6, 3.5, 6.3: weekend => no classes, no group/shift, no periods.
        if (schedule.isWeekend) {
            return DailyUiState(
                date = date,
                dayOfWeek = schedule.dayOfWeek,
                group = null,
                shift = null,
                lessons = emptyList(),
                message = DailyMessage.WEEKEND
            )
        }

        // Req 1.7: a weekday with no periods => no-classes message.
        if (schedule.lessons.isEmpty()) {
            return DailyUiState(
                date = date,
                dayOfWeek = schedule.dayOfWeek,
                group = schedule.group,
                shift = schedule.shift,
                lessons = emptyList(),
                message = DailyMessage.NO_CLASSES
            )
        }

        // Normal weekday with periods.
        return DailyUiState(
            date = date,
            dayOfWeek = schedule.dayOfWeek,
            group = schedule.group,
            shift = schedule.shift,
            lessons = schedule.lessons,
            message = null
        )
    }

    /**
     * Returns the most recent weekday (Mon–Fri) on or before [date]: Saturday
     * steps back to Friday, Sunday steps back to Friday, any weekday is
     * returned unchanged. Used for the clock-failure fallback. (Requirement 6.4)
     */
    private fun mostRecentWeekday(date: LocalDate): LocalDate = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> date.minusDays(1)
        DayOfWeek.SUNDAY -> date.minusDays(2)
        else -> date
    }
}
