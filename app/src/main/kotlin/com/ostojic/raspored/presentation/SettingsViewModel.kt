package com.ostojic.raspored.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostojic.raspored.data.OwnerSaveResult
import com.ostojic.raspored.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI-facing state for the Settings (owner) view, produced by [SettingsViewModel].
 *
 * @param storedOwner the owner currently persisted (kept in sync with
 *   [SettingsRepository.state]); shown as the "current" value on the screen.
 * @param editingText the in-progress text the user is editing in the field;
 *   independent of [storedOwner] until a successful save re-syncs it.
 * @param error the last validation failure ([OwnerError]) or `null` when the
 *   field is valid / untouched since the last edit.
 */
data class SettingsUiState(
    val storedOwner: String,
    val editingText: String,
    val error: OwnerError? = null
)

/**
 * UI-facing owner validation error. Distinct from the data-layer
 * [OwnerSaveResult]: this enum exists purely so the Settings screen can render
 * the right inline message (Req 7.7, 7.8).
 */
enum class OwnerError {
    /** The trimmed input was empty (Req 7.7). */
    EMPTY,

    /** The trimmed input exceeded 50 characters (Req 7.8). */
    TOO_LONG
}

/**
 * Holds and validates the editable owner name for the Settings view.
 *
 * Seeds [SettingsUiState] from the repository's current owner and mirrors later
 * store emissions into [SettingsUiState.storedOwner], leaving the user's
 * in-progress [SettingsUiState.editingText] alone. On a successful
 * [saveOwner] the field is re-synced to the newly stored owner. Validation
 * outcomes from [SettingsRepository.saveOwner] are mapped to [OwnerError] for
 * inline display. (Requirements 7.2, 7.3, 7.4, 7.6, 7.7, 7.8)
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val owner = settingsRepository.state.value.owner
            SettingsUiState(storedOwner = owner, editingText = owner, error = null)
        }
    )

    /** Current Settings view state. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Keep storedOwner in sync with persistence (e.g. a save from elsewhere,
        // or the seed on first run). The user's in-progress editingText is left
        // untouched; only a successful save re-syncs it (see saveOwner).
        viewModelScope.launch {
            settingsRepository.state.collect { persisted ->
                _uiState.value = _uiState.value.copy(storedOwner = persisted.owner)
            }
        }
    }

    /**
     * Records an edit to the owner field: updates [SettingsUiState.editingText]
     * and clears any prior validation [OwnerError]. (Req 7.2, 7.6)
     */
    fun onOwnerTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(editingText = text, error = null)
    }

    /**
     * Validates and persists the current [SettingsUiState.editingText] via
     * [SettingsRepository.saveOwner], mapping the outcome:
     * - [OwnerSaveResult.Saved] → clears [SettingsUiState.error] and re-syncs
     *   [SettingsUiState.editingText] to the stored owner (Req 7.3, 7.4);
     * - [OwnerSaveResult.Empty] → [OwnerError.EMPTY] (Req 7.7);
     * - [OwnerSaveResult.TooLong] → [OwnerError.TOO_LONG] (Req 7.8).
     *
     * On success [SettingsUiState.storedOwner] is updated by the state collector;
     * this method also snaps [SettingsUiState.editingText] to the stored value.
     */
    fun saveOwner() {
        val text = _uiState.value.editingText
        viewModelScope.launch {
            when (settingsRepository.saveOwner(text)) {
                OwnerSaveResult.Saved -> {
                    val owner = settingsRepository.state.value.owner
                    _uiState.value = _uiState.value.copy(
                        storedOwner = owner,
                        editingText = owner,
                        error = null
                    )
                }

                OwnerSaveResult.Empty ->
                    _uiState.value = _uiState.value.copy(error = OwnerError.EMPTY)

                OwnerSaveResult.TooLong ->
                    _uiState.value = _uiState.value.copy(error = OwnerError.TOO_LONG)
            }
        }
    }
}
