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
 * A [TimetableRepository] backed by the compiled-in [TimetableData] constants.
 *
 * Reads are direct lookups into the constant maps. [load] runs a lightweight
 * self-check that can only fail from a programming defect in the tables; it
 * returns [TimetableLoadResult.Unavailable] rather than throwing.
 */
class ConstantTimetableRepository : TimetableRepository {

    override fun lessonsFor(dayOfWeek: DayOfWeek): List<Lesson> =
        TimetableData.LESSONS[dayOfWeek] ?: emptyList()

    override fun bellTimesFor(shift: Shift): Map<Int, BellTime> =
        when (shift) {
            Shift.MORNING -> TimetableData.MORNING_BELLS
            Shift.AFTERNOON -> TimetableData.AFTERNOON_BELLS
        }

    /**
     * Verifies the constant tables are loadable and consistent. (Req 5.7)
     *
     * Returns [TimetableLoadResult.Unavailable] on the first failed invariant,
     * otherwise [TimetableLoadResult.Available]. The valid period range is 1..7.
     */
    override fun load(): TimetableLoadResult {
        val weekdays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        // Invariant 1: every weekday has ≥1 lesson, each within periods 1..7.
        for (day in weekdays) {
            val lessons = TimetableData.LESSONS[day] ?: emptyList()
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
            .flatMap { TimetableData.LESSONS[it] ?: emptyList() }
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
