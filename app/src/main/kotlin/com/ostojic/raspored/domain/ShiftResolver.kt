package com.ostojic.raspored.domain

/**
 * Maps a [DayGroup] and [WeekType] to the [Shift] that group is on for the week.
 *
 * The two groups always sit on opposite shifts, and they swap every week:
 * on an A-week PARNA is on the morning shift and NEPARNA on the afternoon
 * shift; on a B-week the assignment is reversed.
 */
object ShiftResolver {

    /**
     * Resolves the [Shift] for [group] during a week of the given [weekType].
     *
     * WeekType.A → PARNA = MORNING, NEPARNA = AFTERNOON.
     * WeekType.B → PARNA = AFTERNOON, NEPARNA = MORNING.
     */
    fun shiftFor(group: DayGroup, weekType: WeekType): Shift = when (weekType) {
        WeekType.A -> if (group == DayGroup.PARNA) Shift.MORNING else Shift.AFTERNOON
        WeekType.B -> if (group == DayGroup.PARNA) Shift.AFTERNOON else Shift.MORNING
    }
}
