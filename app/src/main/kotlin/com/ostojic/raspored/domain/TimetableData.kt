package com.ostojic.raspored.domain

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * The fixed, hard-coded weekly timetable data.
 *
 * This is the single source of truth for the school layout: the lessons taught
 * on each weekday and the bell (clock) times for the morning and afternoon
 * shifts. It has no Android dependency so it can be consumed by the pure
 * domain rules and exercised in unit/property tests.
 */
object TimetableData {

    /**
     * The fixed weekly lesson layout keyed by [DayOfWeek].
     *
     * A [Lesson] with a `null` classCode denotes a pause. Weekends map to empty
     * lists.
     */
    val LESSONS: Map<DayOfWeek, List<Lesson>> = mapOf(
        DayOfWeek.MONDAY to listOf(
            Lesson(6, "5/4"), Lesson(7, "5/2")
        ),
        DayOfWeek.TUESDAY to listOf(
            Lesson(2, "6/1"), Lesson(3, "8/5"), Lesson(4, "6/3"),
            Lesson(5, "6/5"), Lesson(6, "7/1"), Lesson(7, "7/5")
        ),
        DayOfWeek.WEDNESDAY to listOf(
            Lesson(1, "7/3"), Lesson(2, "5/3"), Lesson(3, "8/1"),
            Lesson(4, null),  Lesson(5, "6/7"), Lesson(6, "8/7")   // period 4 = Pause
        ),
        DayOfWeek.THURSDAY to listOf(
            Lesson(1, "6/4"), Lesson(2, "6/2"), Lesson(3, "6/6"),
            Lesson(4, "8/4"), Lesson(5, null),  Lesson(6, "7/4"),  // period 5 = Pause
            Lesson(7, "7/2")
        ),
        DayOfWeek.FRIDAY to listOf(
            Lesson(4, "7/6"), Lesson(5, "8/2"), Lesson(6, "8/6"), Lesson(7, "7/7")
        ),
        DayOfWeek.SATURDAY to emptyList(),
        DayOfWeek.SUNDAY to emptyList()
    )

    // Morning shift bell times. (Req 5.3)
    val MORNING_BELLS: Map<Int, BellTime> = mapOf(
        1 to BellTime(1, LocalTime.of(8, 0),  LocalTime.of(8, 45)),
        2 to BellTime(2, LocalTime.of(8, 50), LocalTime.of(9, 35)),
        3 to BellTime(3, LocalTime.of(9, 55), LocalTime.of(10, 40)),
        4 to BellTime(4, LocalTime.of(10, 45), LocalTime.of(11, 30)),
        5 to BellTime(5, LocalTime.of(11, 35), LocalTime.of(12, 20)),
        6 to BellTime(6, LocalTime.of(12, 25), LocalTime.of(13, 10)),
        7 to BellTime(7, LocalTime.of(13, 15), LocalTime.of(14, 0))
    )

    // Afternoon shift bell times. (Req 5.4)
    val AFTERNOON_BELLS: Map<Int, BellTime> = mapOf(
        1 to BellTime(1, LocalTime.of(14, 0),  LocalTime.of(14, 45)),
        2 to BellTime(2, LocalTime.of(14, 50), LocalTime.of(15, 35)),
        3 to BellTime(3, LocalTime.of(15, 55), LocalTime.of(16, 40)),
        4 to BellTime(4, LocalTime.of(16, 45), LocalTime.of(17, 30)),
        5 to BellTime(5, LocalTime.of(17, 35), LocalTime.of(18, 20)),
        6 to BellTime(6, LocalTime.of(18, 25), LocalTime.of(19, 10)),
        7 to BellTime(7, LocalTime.of(19, 15), LocalTime.of(20, 0))
    )
}
