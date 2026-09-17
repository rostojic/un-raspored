package com.ostojic.raspored.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek

/**
 * Serializable persistence models for the school timetable.
 *
 * These stay in the pure domain layer (no Android imports) so they remain
 * unit-testable. [java.time.DayOfWeek] is a Java enum, so kotlinx.serialization
 * has no built-in serializer for it; [DayOfWeekSerializer] encodes it by its
 * enum `name` (MONDAY..SUNDAY). [DayGroup] is a Kotlin enum and serializes by
 * name automatically.
 */

/**
 * Serializes a [DayOfWeek] as its enum name (e.g. "MONDAY").
 *
 * Used for map keys in [StoredSchedule] because kotlinx.serialization does not
 * provide a built-in serializer for the Java `java.time.DayOfWeek` enum.
 */
object DayOfWeekSerializer : KSerializer<DayOfWeek> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.DayOfWeek", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DayOfWeek) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): DayOfWeek =
        DayOfWeek.valueOf(decoder.decodeString())
}

/**
 * A persistable snapshot of a full weekly schedule.
 *
 * @param lessonsByDay the fixed weekly lesson layout keyed by weekday. A
 *   [Lesson] with a `null` classCode denotes a pause.
 * @param groupByDay the [DayGroup] each weekday belongs to.
 */
@Serializable
data class StoredSchedule(
    val lessonsByDay: Map<@Serializable(DayOfWeekSerializer::class) DayOfWeek, List<Lesson>>,
    val groupByDay: Map<@Serializable(DayOfWeekSerializer::class) DayOfWeek, DayGroup>
)

/**
 * The complete persisted application state.
 *
 * @param owner the schedule owner's name.
 * @param current the active schedule.
 * @param previous the prior schedule, if any (used to detect/restore changes).
 */
@Serializable
data class PersistedState(
    val owner: String,
    val current: StoredSchedule,
    val previous: StoredSchedule? = null
)

/** The seed owner name for a fresh install. */
const val DEFAULT_OWNER: String = "Olivera"

/**
 * The seed schedule for a fresh install, built from [TimetableData.LESSONS]
 * (weekends excluded) and the fixed [DAY_GROUP] membership map.
 */
val DEFAULT_SCHEDULE: StoredSchedule = StoredSchedule(
    lessonsByDay = TimetableData.LESSONS.filterKeys {
        it != DayOfWeek.SATURDAY && it != DayOfWeek.SUNDAY
    },
    groupByDay = DAY_GROUP
)
