package com.ostojic.raspored.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pure domain layer for the school timetable.
 *
 * These types have no Android dependency so the business rules (WeekType
 * calculation, shift resolution, schedule assembly) can be unit- and
 * property-tested in isolation.
 */

/** Which of the two alternating weekly rotations a given week falls in. */
enum class WeekType { A, B }

/**
 * Fixed partition of the weekdays into two groups.
 *
 * PARNA = Monday, Thursday, Friday. NEPARNA = Tuesday, Wednesday.
 */
enum class DayGroup { PARNA, NEPARNA }

/** The daily shift a group is on for a given week. */
enum class Shift { MORNING, AFTERNOON }

/**
 * A single teaching period as stored in the fixed weekly layout.
 *
 * @param period the period number (1..7).
 * @param classCode the class/group code taught (e.g. "5/4"); `null` denotes a
 *   pause (no class that period).
 */
data class Lesson(
    val period: Int,          // 1..7
    val classCode: String?    // e.g. "5/4"; null => Pause
) {
    val isPause: Boolean get() = classCode == null
}

/** The clock start/end times for a period within a particular shift. */
data class BellTime(
    val period: Int,
    val start: LocalTime,
    val end: LocalTime
)

/**
 * A period after its shift-specific bell times have been applied.
 *
 * @param classCode the class/group code taught; `null` denotes a pause.
 */
data class ResolvedLesson(
    val period: Int,
    val classCode: String?,   // null => Pause
    val start: LocalTime,
    val end: LocalTime
) {
    val isPause: Boolean get() = classCode == null
}

/**
 * The fully resolved schedule for one calendar date.
 *
 * @param group `null` on weekends.
 * @param shift `null` on weekends.
 * @param lessons empty on weekends and on weekdays with no periods.
 */
data class DaySchedule(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val isWeekend: Boolean,
    val group: DayGroup?,           // null on weekends
    val weekType: WeekType,
    val shift: Shift?,              // null on weekends
    val lessons: List<ResolvedLesson>
)

/** Non-error messages shown in place of a period list. */
enum class DailyMessage { WEEKEND, NO_CLASSES, DATA_UNAVAILABLE, DATE_UNDETERMINED }

/**
 * Fixed membership of each weekday in a [DayGroup].
 *
 * Monday, Thursday, Friday are PARNA; Tuesday, Wednesday are NEPARNA. Weekends
 * are intentionally absent.
 */
val DAY_GROUP: Map<DayOfWeek, DayGroup> = mapOf(
    DayOfWeek.MONDAY    to DayGroup.PARNA,
    DayOfWeek.THURSDAY  to DayGroup.PARNA,
    DayOfWeek.FRIDAY    to DayGroup.PARNA,
    DayOfWeek.TUESDAY   to DayGroup.NEPARNA,
    DayOfWeek.WEDNESDAY to DayGroup.NEPARNA
)
