package com.ostojic.raspored.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Assembles the fully resolved [DaySchedule] for any calendar date.
 *
 * Composes the domain rules end to end: [WeekTypeCalculator] fixes the week's
 * [WeekType], the stored group assignment via [TimetableRepository.groupFor]
 * (falling back to [DAY_GROUP]) maps the weekday to its [DayGroup],
 * [ShiftResolver] picks the [Shift], and the [TimetableRepository] supplies the
 * WeekType-independent lessons plus the shift-specific bell times.
 *
 * Class codes are taken solely from [TimetableRepository.lessonsFor], which is
 * WeekType-independent, so only the clock times vary between WeekType "A" and
 * "B" (Req 2.8). Pure Kotlin with no Android dependency.
 */
class ScheduleService(private val repository: TimetableRepository) {

    /**
     * Resolves the schedule for [date].
     *
     * Weekends return an empty [DaySchedule] with `isWeekend = true` and null
     * group/shift. Weekdays resolve the group, shift and bell times, then map
     * each lesson (sorted ascending by period, Req 1.2) to a [ResolvedLesson]
     * carrying the shift's start/end times (Req 1.1, 2.7). Pauses before the
     * first and after the last taught period are dropped; see [trimEdgePauses].
     */
    fun scheduleFor(date: LocalDate): DaySchedule {
        val dow = date.dayOfWeek
        val weekType = WeekTypeCalculator.weekTypeFor(date)
        val isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
        if (isWeekend) {
            return DaySchedule(date, dow, true, group = null, weekType, shift = null, lessons = emptyList())
        }
        val group = repository.groupFor(dow) ?: DAY_GROUP.getValue(dow)
        val shift = ShiftResolver.shiftFor(group, weekType)
        val bells = repository.bellTimesFor(shift)              // Req 2.7 applies shift bell times
        val lessons = repository.lessonsFor(dow)                // fixed, WeekType-independent (Req 2.8)
            .sortedBy { it.period }                             // ascending order (Req 1.2)
            .trimEdgePauses()                                   // pauses only between classes
            .map { l ->
                val b = bells.getValue(l.period)
                ResolvedLesson(l.period, l.classCode, b.start, b.end)
            }
        return DaySchedule(date, dow, false, group, weekType, shift, lessons)
    }

    /**
     * Drops the pauses that sit before the first and after the last taught
     * period of a day.
     *
     * A pause is only meaningful as a gap *between* classes. The schedule
     * editor stores every weekday as all seven periods 1..7, padding the
     * untaught ones with pauses, so without this trim a day that starts at
     * period 6 would render five leading "no class" rows before its first
     * actual slot. Interior pauses are preserved; a day with no taught period
     * at all collapses to an empty list, which the Daily view reports as
     * "no classes".
     *
     * The receiver is expected to already be sorted ascending by period.
     */
    private fun List<Lesson>.trimEdgePauses(): List<Lesson> {
        val first = indexOfFirst { !it.isPause }
        if (first < 0) return emptyList()
        val last = indexOfLast { !it.isPause }
        return subList(first, last + 1)
    }
}
