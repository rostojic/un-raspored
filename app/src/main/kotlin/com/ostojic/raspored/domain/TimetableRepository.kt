package com.ostojic.raspored.domain

import java.time.DayOfWeek

/**
 * Provides the fixed timetable and bell times from local constant data.
 *
 * Returns a `Result`-style outcome from [load] so the startup "data
 * unavailable" path (Requirement 5.7) is representable even though the data is
 * hard-coded. Pure Kotlin with no Android dependency.
 */
interface TimetableRepository {

    /** Ordered lessons for a weekday, independent of WeekType. Empty for weekends. (Req 5.1, 5.2) */
    fun lessonsFor(dayOfWeek: DayOfWeek): List<Lesson>

    /**
     * The [DayGroup] assigned to a weekday in the current [StoredSchedule].
     *
     * Returns `null` for weekends and any day not present in the assignment.
     * (Req 8.6, 8.13)
     */
    fun groupFor(dayOfWeek: DayOfWeek): DayGroup?

    /** Bell times for a shift, keyed by period number. (Req 5.3, 5.4) */
    fun bellTimesFor(shift: Shift): Map<Int, BellTime>

    /** Verifies the constant tables are loadable/consistent at startup. (Req 5.7) */
    fun load(): TimetableLoadResult
}

/** Outcome of a startup consistency check on the stored timetable data. */
sealed interface TimetableLoadResult {
    /** The constant tables are present and internally consistent. */
    data object Available : TimetableLoadResult

    /** The tables failed a self-check; [reason] describes the first failure. */
    data class Unavailable(val reason: String) : TimetableLoadResult
}

/**
 * A [TimetableRepository] backed by an in-memory [StoredSchedule] snapshot.
 *
 * Lesson and group reads resolve against a mutable snapshot that defaults to
 * [DEFAULT_SCHEDULE] and is refreshed via [updateSnapshot] (the target the
 * `SettingsRepository` snapshot sink calls when persisted edits change). Bell
 * times remain sourced from the compiled-in [TimetableData] constants. [load]
 * runs a lightweight self-check against the current snapshot; it returns
 * [TimetableLoadResult.Unavailable] rather than throwing.
 */
class ConstantTimetableRepository : TimetableRepository {

    /** The current schedule snapshot; starts from the seed [DEFAULT_SCHEDULE]. */
    @Volatile
    private var snapshot: StoredSchedule = DEFAULT_SCHEDULE

    /**
     * Replaces the in-memory [snapshot] with [schedule].
     *
     * Wired as the `SettingsRepository` snapshot sink (task 25) so persisted
     * user edits become visible to subsequent [lessonsFor]/[groupFor] reads.
     */
    fun updateSnapshot(schedule: StoredSchedule) {
        snapshot = schedule
    }

    override fun lessonsFor(dayOfWeek: DayOfWeek): List<Lesson> =
        snapshot.lessonsByDay[dayOfWeek] ?: emptyList()

    override fun groupFor(dayOfWeek: DayOfWeek): DayGroup? =
        snapshot.groupByDay[dayOfWeek]

    override fun bellTimesFor(shift: Shift): Map<Int, BellTime> =
        when (shift) {
            Shift.MORNING -> TimetableData.MORNING_BELLS
            Shift.AFTERNOON -> TimetableData.AFTERNOON_BELLS
        }

    /**
     * Verifies the current snapshot is loadable and consistent. (Req 5.7)
     *
     * Returns [TimetableLoadResult.Unavailable] on the first failed invariant,
     * otherwise [TimetableLoadResult.Available]. The valid period range is 1..7.
     * Lesson invariants read from the current [snapshot]; bell-time invariants
     * read from the [TimetableData] bell constants.
     */
    override fun load(): TimetableLoadResult {
        val current = snapshot
        val weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        // Invariant 1: every weekday has ≥1 lesson, each within periods 1..7.
        for (day in weekdays) {
            val lessons = current.lessonsByDay[day] ?: emptyList()
            if (lessons.isEmpty()) {
                return TimetableLoadResult.Unavailable("No lessons defined for $day")
            }
            for (lesson in lessons) {
                if (lesson.period !in 1..7) {
                    return TimetableLoadResult.Unavailable(
                        "Period ${lesson.period} on $day is outside the valid range 1..7"
                    )
                }
            }
        }

        // Invariant 2: every referenced period has a bell time in both shifts.
        val referencedPeriods = weekdays
            .flatMap { current.lessonsByDay[it] ?: emptyList() }
            .map { it.period }
            .toSortedSet()
        for (period in referencedPeriods) {
            if (period !in TimetableData.MORNING_BELLS) {
                return TimetableLoadResult.Unavailable("No morning bell time for period $period")
            }
            if (period !in TimetableData.AFTERNOON_BELLS) {
                return TimetableLoadResult.Unavailable("No afternoon bell time for period $period")
            }
        }

        return TimetableLoadResult.Available
    }
}
