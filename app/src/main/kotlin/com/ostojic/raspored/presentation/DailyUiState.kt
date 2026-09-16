package com.ostojic.raspored.presentation

import com.ostojic.raspored.domain.DailyMessage
import com.ostojic.raspored.domain.DayGroup
import com.ostojic.raspored.domain.ResolvedLesson
import com.ostojic.raspored.domain.Shift
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * UI-facing state for the Daily view, produced by [ScheduleViewModel].
 *
 * ## Deviation from design.md field types (intentional)
 *
 * The design lists `headerLabel: String`, `groupLabel: String?` and
 * `shiftLabel: String?` as already-localized strings. This implementation
 * instead carries the raw semantic values ([dayOfWeek], [group], [shift]) and
 * lets the Composable resolve the localized Serbian strings via
 * `stringResource` (`day_monday`..`day_sunday`, `group_parna`/`group_neparna`,
 * `shift_morning`/`shift_afternoon`, `message_*`).
 *
 * Rationale: a [androidx.lifecycle.ViewModel] must not hold an Android
 * `Context` or call `stringResource`, otherwise it could not be unit-tested
 * with only an injected fixed `Clock` and no Android runtime (task 10.2). Keeping
 * the state purely semantic preserves the design intent (the same data reaches
 * the UI) while leaving the ViewModel a pure JVM component.
 *
 * @param group `null` on weekends / when no group applies.
 * @param shift `null` on weekends / when no shift applies.
 * @param lessons empty on weekends, on weekdays with no periods, and when data
 *   is unavailable.
 * @param message a non-null [DailyMessage] shown in place of the period list
 *   (weekend, no classes, data unavailable, or date undetermined); `null` when a
 *   normal period list is shown.
 */
data class DailyUiState(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val group: DayGroup?,
    val shift: Shift?,
    val lessons: List<ResolvedLesson>,
    val message: DailyMessage?
)
