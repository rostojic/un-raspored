package com.ostojic.raspored.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ostojic.raspored.domain.PersistedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * The single device-local I/O point for the persisted application state.
 *
 * Wraps a [DataStore] of [Preferences], storing the whole [PersistedState] as a
 * JSON string under one key. The [DataStore] instance is injected (rather than
 * created via the `preferencesDataStore` delegate) so this class stays testable;
 * the delegate/Context wiring happens at app startup in MainActivity.
 */
class SchedulePreferencesStore(private val dataStore: DataStore<Preferences>) {

    /**
     * Emits the decoded [PersistedState], or `null` when nothing is stored, the
     * stored value cannot be decoded (corrupt/incompatible JSON), or the read
     * fails with an [IOException]. Callers treat `null` as "nothing readable
     * stored" and seed/reseed accordingly (Req 8.11).
     */
    val state: Flow<PersistedState?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            val encoded = preferences[KEY] ?: return@map null
            try {
                json.decodeFromString<PersistedState>(encoded)
            } catch (_: Exception) {
                // Parse/decoding failure => treat as nothing readable stored.
                null
            }
        }

    /**
     * Serializes and atomically writes the whole [state]. Propagates any write
     * failure to the caller (the [SchedulePreferencesStore] never swallows write
     * errors; SettingsRepository maps them to a WriteFailed result, Req 8.9).
     */
    suspend fun write(state: PersistedState) {
        val encoded = json.encodeToString(state)
        dataStore.edit { preferences ->
            preferences[KEY] = encoded
        }
    }

    /**
     * Reports whether the persisted-state key is present in storage, regardless
     * of whether its value can be decoded.
     *
     * [state] maps both an absent key and an undecodable value to `null`, so it
     * cannot on its own tell a genuine first run (nothing ever stored) from
     * corruption (a key exists but its JSON is unparseable). [SettingsRepository]
     * uses this to choose between seeding (`SeededDefault`, Req 8.1) and
     * reseeding after corruption (`ReseededAfterCorruption`, Req 8.11). A failed
     * read is treated as "no key present".
     */
    suspend fun rawExists(): Boolean =
        try {
            dataStore.data
                .catch { throwable ->
                    if (throwable is IOException) emit(emptyPreferences()) else throw throwable
                }
                .first()[KEY] != null
        } catch (_: IOException) {
            false
        }

    private companion object {
        val KEY = stringPreferencesKey("persisted_state")

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
