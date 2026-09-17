package com.ostojic.raspored.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostojic.raspored.data.RevertResult
import com.ostojic.raspored.data.ScheduleSaveResult
import com.ostojic.raspored.data.SettingsRepository
import com.ostojic.raspored.domain.DayGroup
import com.ostojic.raspored.domain.Lesson
import com.ostojic.raspored.domain.StoredSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/** A per-cell validation error for a class-code field. */
enum class ClassCodeError { TOO_LONG }

/** One-shot messages surfaced to the editor UI after an action completes. */
enum class EditorNotice { SAVED, REVERTED, NO_PREVIOUS }

/**
 * View state for the Schedule Editor.
 *
 * @param working the in-progress working copy of the schedule; normalized so
 *   every weekday Monday–Friday holds exactly seven [Lesson] slots for periods
 *   1..7 (missing periods filled with a Pause). Only this copy is mutated while
 *   editing; the [StoredSchedule] on the device stays untouched until [save]
 *   (Req 8.7).
 * @param hasPrevious whether a previous schedule exists to revert to (Req 9.3, 9.6).
 * @param classCodeErrors per-cell validation errors keyed by (weekday, period)
 *   (Req 8.4).
 * @param saveError a non-null message when the last save failed (Req 8.9).
 * @param notice a one-shot notice to display, cleared via [ScheduleEditorViewModel.consumeNotice].
 */
data class EditorUiState(
    val working: StoredSchedule,
    val hasPrevious: Boolean,
    val classCodeErrors: Map<Pair<DayOfWeek, Int>, ClassCodeError> = emptyMap(),
    val saveError: String? = null,
    val notice: EditorNotice? = null
)

/**
 * Holds the editable working copy of the [StoredSchedule] and the editor rules.
 *
 * The working copy is initialized from [SettingsRepository.state]'s current
 * value and normalized so each weekday Monday–Friday exposes all seven periods
 * (1..7) as editable rows (Req 8.2), filling any missing period with a Pause.
 * Edits mutate only this copy; the device-local [StoredSchedule] is left
 * unchanged until [save] (Req 8.7). [save] and [revert] delegate to
 * [SettingsRepository], which owns persistence and the single previous-schedule
 * retention rule. (Requirement 8, 9)
 */
class ScheduleEditorViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<EditorUiState>

    /** Current editor view state. */
    val uiState: StateFlow<EditorUiState>

    init {
        val persisted = settingsRepository.state.value
        _uiState = MutableStateFlow(
            EditorUiState(
                working = normalize(persisted.current),
                hasPrevious = persisted.previous != null
            )
        )
        uiState = _uiState.asStateFlow()
    }

    /**
     * Applies an edit to the class code of ([day], [period]) in the working copy.
     *
     * The input is trimmed. A trimmed length greater than 20 flags a [ClassCodeError.TOO_LONG]
     * for that cell and leaves the working value unchanged (Req 8.4). Otherwise any
     * existing error for the cell is cleared and the cell is set to the trimmed
     * value, or to a Pause when the trimmed input is empty (Req 8.5). Only the
     * targeted cell changes; all other entries and the per-period order are
     * preserved.
     */
    fun onClassCodeChanged(day: DayOfWeek, period: Int, text: String) {
        val trimmed = text.trim()
        val current = _uiState.value
        val key = day to period

        if (trimmed.length > 20) {
            // Req 8.4: reject over-long input; keep the working value as-is.
            _uiState.value = current.copy(
                classCodeErrors = current.classCodeErrors + (key to ClassCodeError.TOO_LONG)
            )
            return
        }

        // Req 8.5: blank => Pause (null classCode); otherwise the trimmed value.
        val newCode: String? = trimmed.ifEmpty { null }
        val updatedDay = current.working.lessonsByDay[day].orEmpty().map { lesson ->
            if (lesson.period == period) lesson.copy(classCode = newCode) else lesson
        }
        val newLessonsByDay = current.working.lessonsByDay + (day to updatedDay)

        _uiState.value = current.copy(
            working = current.working.copy(lessonsByDay = newLessonsByDay),
            classCodeErrors = current.classCodeErrors - key
        )
    }

    /** Sets the [group] assignment of [day] in the working copy (Req 8.6). */
    fun onGroupToggled(day: DayOfWeek, group: DayGroup) {
        val current = _uiState.value
        val newGroupByDay = current.working.groupByDay + (day to group)
        _uiState.value = current.copy(
            working = current.working.copy(groupByDay = newGroupByDay)
        )
    }

    /**
     * Persists the working copy via [SettingsRepository.saveSchedule].
     *
     * On success the working copy (all seven periods per weekday, pauses
     * included, Req 8.8) becomes the current stored schedule, a [EditorNotice.SAVED]
     * notice is surfaced, any prior save error is cleared, and [EditorUiState.hasPrevious]
     * becomes true (the pre-save current is retained as previous). On failure the
     * stored schedule is left unchanged and an error message is shown (Req 8.9).
     */
    fun save() {
        viewModelScope.launch {
            when (val result = settingsRepository.saveSchedule(_uiState.value.working)) {
                ScheduleSaveResult.Saved -> {
                    _uiState.value = _uiState.value.copy(
                        hasPrevious = true,
                        saveError = null,
                        notice = EditorNotice.SAVED
                    )
                }
                is ScheduleSaveResult.WriteFailed -> {
                    _uiState.value = _uiState.value.copy(saveError = result.reason)
                }
            }
        }
    }

    /**
     * Restores the previous schedule via [SettingsRepository.revert].
     *
     * When a previous schedule existed it is restored and discarded: the working
     * copy is re-initialized (normalized to seven periods per weekday), [EditorUiState.hasPrevious]
     * becomes false, and a [EditorNotice.REVERTED] notice is surfaced. When no
     * previous schedule exists the stored schedule is untouched and a
     * [EditorNotice.NO_PREVIOUS] notice is surfaced (Req 9.3, 9.6). A write
     * failure surfaces an error message.
     */
    fun revert() {
        viewModelScope.launch {
            when (val result = settingsRepository.revert()) {
                RevertResult.Reverted -> {
                    _uiState.value = _uiState.value.copy(
                        working = normalize(settingsRepository.state.value.current),
                        hasPrevious = false,
                        saveError = null,
                        notice = EditorNotice.REVERTED
                    )
                }
                RevertResult.NoPrevious -> {
                    _uiState.value = _uiState.value.copy(notice = EditorNotice.NO_PREVIOUS)
                }
                is RevertResult.WriteFailed -> {
                    _uiState.value = _uiState.value.copy(saveError = result.reason)
                }
            }
        }
    }

    /** Clears the current one-shot [EditorNotice] after the UI has shown it. */
    fun consumeNotice() {
        if (_uiState.value.notice != null) {
            _uiState.value = _uiState.value.copy(notice = null)
        }
    }

    private companion object {
        /** The weekdays the editor exposes, in display order. */
        val EDITABLE_DAYS: List<DayOfWeek> = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        /** The period numbers exposed per weekday. */
        val PERIODS: IntRange = 1..7

        /**
         * Returns a copy of [source] where every weekday Monday–Friday maps to a
         * list of seven [Lesson]s for periods 1..7 in ascending order. Existing
         * lessons are preserved; any missing period is filled with a Pause
         * ([Lesson] with a null classCode) so the editor can render all seven
         * rows (Req 8.2). Group assignments are carried over unchanged.
         */
        fun normalize(source: StoredSchedule): StoredSchedule {
            val normalizedLessons = EDITABLE_DAYS.associateWith { day ->
                val existing = source.lessonsByDay[day].orEmpty().associateBy { it.period }
                PERIODS.map { period ->
                    existing[period] ?: Lesson(period = period, classCode = null)
                }
            }
            return source.copy(lessonsByDay = normalizedLessons)
        }
    }
}
