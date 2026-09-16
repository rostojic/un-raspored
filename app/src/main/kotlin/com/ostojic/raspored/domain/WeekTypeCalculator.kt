package com.ostojic.raspored.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Computes the alternating [WeekType] (A/B) for any calendar date.
 *
 * Weeks are anchored to [ANCHOR_MONDAY], which is defined as a `WeekType.A`
 * week. Every date in the same Monday–Sunday week shares one [WeekType], and
 * consecutive weeks alternate. Pure Kotlin with no Android dependency so the
 * rule can be unit- and property-tested in isolation.
 */
object WeekTypeCalculator {

    /** Monday of the anchor week; defined as WeekType.A. */
    val ANCHOR_MONDAY: LocalDate = LocalDate.of(2026, 8, 31)

    /**
     * Absolute count of whole Mon–Sun weeks between [date]'s Monday and the
     * anchor Monday. Handles dates before the anchor via [Math.floorDiv] and an
     * absolute value.
     */
    fun weeksFromAnchor(date: LocalDate): Long {
        // Monday of the given date's week (java.time weeks start Monday).
        val mondayOfDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        // Whole-week distance from the anchor Monday; may be negative for earlier dates.
        val days = ChronoUnit.DAYS.between(ANCHOR_MONDAY, mondayOfDate)
        return Math.floorDiv(days, 7).let { Math.abs(it) }
    }

    /**
     * The [WeekType] for [date]: `A` for an even whole-week distance from the
     * anchor, `B` for an odd distance.
     */
    fun weekTypeFor(date: LocalDate): WeekType =
        if (weeksFromAnchor(date) % 2 == 0L) WeekType.A else WeekType.B
}
