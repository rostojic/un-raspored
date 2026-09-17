package com.ostojic.raspored.data

import com.ostojic.raspored.domain.DEFAULT_OWNER
import com.ostojic.raspored.domain.DEFAULT_SCHEDULE
import com.ostojic.raspored.domain.PersistedState
import com.ostojic.raspored.domain.StoredSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns all editable, persisted application state (owner, current schedule, and
 * the single previous schedule) and the seed/reseed/save/revert rules.
 *
 * It is the only collaborator the ViewModels talk to for persistence. It reads
 * and writes through [SchedulePreferencesStore] (the sole device-local I/O
 * point) and keeps [com.ostojic.raspored.domain.TimetableRepository]'s in-memory
 * snapshot in sync by pushing each current [StoredSchedule] through
 * [snapshotSink] whenever it changes. Bell times are never part of this
 * repository. (Requirements 7, 8, 9)
 *
 * @param store the DataStore-backed persistence point.
 * @param snapshotSink a callback that updates the TimetableRepository snapshot
 *   whenever the current schedule changes.
 * @param scope the scope used to passively collect [SchedulePreferencesStore.state];
 *   injected so tests can supply a controlled/test scope.
 */
class SettingsRepository(
    private val store: SchedulePreferencesStore,
    private val snapshotSink: (StoredSchedule) -> Unit,
    scope: CoroutineScope
) {

    private val _state = MutableStateFlow(
        PersistedState(owner = DEFAULT_OWNER, current = DEFAULT_SCHEDULE)
    )

    /**
     * Owner + current schedule + whether a previous schedule exists, refreshed
     * from DataStore. Seeded with a safe default until [initialize] runs.
     */
    val state: StateFlow<PersistedState> = _state.asStateFlow()

    init {
        // Passively mirror store emissions into state + snapshot. Nulls (absent
        // or corrupt) are ignored here; seeding/reseeding is handled by
        // initialize(), which distinguishes first-run from corruption.
        scope.launch {
            store.state.collect { persisted ->
                if (persisted != null) {
                    _state.value = persisted
                    snapshotSink(persisted.current)
                }
            }
        }
    }

    /**
     * Establishes the initial persisted state on startup.
     *
     * Reads the current stored value once:
     * - valid state present → adopts it, returns [InitOutcome.Ok];
     * - nothing stored (genuine first run) → seeds [DEFAULT_SCHEDULE]/[DEFAULT_OWNER],
     *   returns [InitOutcome.SeededDefault] (Req 8.1);
     * - stored data present but unreadable/unparseable → reseeds the defaults
     *   and returns [InitOutcome.ReseededAfterCorruption] (Req 8.11).
     *
     * In every case it updates [state] and pushes the current schedule through
     * [snapshotSink] so the TimetableRepository snapshot is current (Req 5.6, 5.7).
     */
    suspend fun initialize(): InitOutcome {
        val current = store.state.first()
        if (current != null) {
            _state.value = current
            snapshotSink(current.current)
            return InitOutcome.Ok
        }

        // null => nothing readable. Distinguish absent (first run) from corrupt
        // (a key exists but failed to parse) via the store's raw presence check.
        val hadStoredData = store.rawExists()
        val seeded = PersistedState(owner = DEFAULT_OWNER, current = DEFAULT_SCHEDULE)
        store.write(seeded)
        _state.value = seeded
        snapshotSink(seeded.current)
        return if (hadStoredData) {
            InitOutcome.ReseededAfterCorruption
        } else {
            InitOutcome.SeededDefault
        }
    }

    /**
     * Validates and persists the owner name (Req 7.3, 7.4, 7.7, 7.8).
     *
     * Trims [raw]; an empty result returns [OwnerSaveResult.Empty] and a result
     * longer than 50 characters returns [OwnerSaveResult.TooLong], both leaving
     * the stored owner unchanged. A valid trimmed value replaces the prior owner
     * and returns [OwnerSaveResult.Saved]. The current schedule is untouched, so
     * the snapshot is not refreshed.
     */
    suspend fun saveOwner(raw: String): OwnerSaveResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return OwnerSaveResult.Empty
        if (trimmed.length > 50) return OwnerSaveResult.TooLong

        val next = _state.value.copy(owner = trimmed)
        store.write(next)
        _state.value = next
        return OwnerSaveResult.Saved
    }

    /**
     * Replaces the current schedule with [edited], retaining exactly one
     * previous schedule (the pre-save current) (Req 8.8, 9.1, 9.2).
     *
     * On a write failure the stored state is left unchanged and the failure is
     * surfaced as [ScheduleSaveResult.WriteFailed] (Req 8.9); neither [state]
     * nor the snapshot is updated in that case.
     */
    suspend fun saveSchedule(edited: StoredSchedule): ScheduleSaveResult {
        val cur = _state.value
        val next = cur.copy(current = edited, previous = cur.current)
        return try {
            store.write(next)
            _state.value = next
            snapshotSink(edited)
            ScheduleSaveResult.Saved
        } catch (e: Exception) {
            ScheduleSaveResult.WriteFailed(e.message ?: "write failed")
        }
    }

    /**
     * Restores the previous schedule and discards it in a single write, leaving
     * the owner unchanged (Req 9.3, 9.4, 9.5).
     *
     * With no previous schedule it writes nothing and returns
     * [RevertResult.NoPrevious] (Req 9.6); a second consecutive revert therefore
     * finds no previous. A write failure leaves the stored state unchanged and
     * returns [RevertResult.WriteFailed].
     */
    suspend fun revert(): RevertResult {
        val cur = _state.value
        val prev = cur.previous ?: return RevertResult.NoPrevious
        val next = cur.copy(current = prev, previous = null)
        return try {
            store.write(next)
            _state.value = next
            snapshotSink(prev)
            RevertResult.Reverted
        } catch (e: Exception) {
            RevertResult.WriteFailed(e.message ?: "write failed")
        }
    }
}

/** Outcome of [SettingsRepository.initialize]. */
sealed interface InitOutcome {
    /** A valid persisted state was already present and adopted. */
    data object Ok : InitOutcome

    /** Nothing was stored; the defaults were seeded (Req 8.1). */
    data object SeededDefault : InitOutcome

    /** Stored data was unreadable/unparseable; the defaults were reseeded (Req 8.11). */
    data object ReseededAfterCorruption : InitOutcome
}

/** Outcome of [SettingsRepository.saveOwner]. */
sealed interface OwnerSaveResult {
    /** The trimmed owner (length 1..50) was stored. */
    data object Saved : OwnerSaveResult

    /** The trimmed input was empty; the stored owner is unchanged (Req 7.7). */
    data object Empty : OwnerSaveResult

    /** The trimmed input exceeded 50 characters; the stored owner is unchanged (Req 7.8). */
    data object TooLong : OwnerSaveResult
}

/** Outcome of [SettingsRepository.saveSchedule]. */
sealed interface ScheduleSaveResult {
    /** The edited schedule was stored, retaining the prior current as previous. */
    data object Saved : ScheduleSaveResult

    /** The write failed; the stored state is unchanged (Req 8.9). */
    data class WriteFailed(val reason: String) : ScheduleSaveResult
}

/** Outcome of [SettingsRepository.revert]. */
sealed interface RevertResult {
    /** The previous schedule was restored and discarded. */
    data object Reverted : RevertResult

    /** No previous schedule existed; nothing was written (Req 9.6). */
    data object NoPrevious : RevertResult

    /** The write failed; the stored state is unchanged. */
    data class WriteFailed(val reason: String) : RevertResult
}
