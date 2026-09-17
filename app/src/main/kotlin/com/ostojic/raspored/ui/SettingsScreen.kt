package com.ostojic.raspored.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ostojic.raspored.R
import com.ostojic.raspored.presentation.OwnerError
import com.ostojic.raspored.presentation.SettingsUiState
import com.ostojic.raspored.ui.theme.RasporedTheme

/**
 * Stateless Settings view. Renders the owner name in an editable field with
 * inline validation and a save action; the hosting Activity/ViewModel owns the
 * state. (Requirements 7.2, 7.4, 7.6, 7.7, 7.8)
 *
 * Rendering rules:
 * - The field is bound to [SettingsUiState.editingText] and reports edits via
 *   [onOwnerTextChanged] (Req 7.2, 7.6).
 * - When [SettingsUiState.error] is set, the field shows an error state and an
 *   inline message: [OwnerError.EMPTY] → empty-name message (Req 7.7),
 *   [OwnerError.TOO_LONG] → over-50 message (Req 7.8).
 * - With no error, the supporting text shows the currently stored owner
 *   ([SettingsUiState.storedOwner]) (Req 7.4).
 * - The Save action calls [onSave]; the back navigation calls [onBack].
 *
 * All visible strings are Serbian Cyrillic string resources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOwnerTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val error = state.error
            OutlinedTextField(
                value = state.editingText,
                onValueChange = onOwnerTextChanged,
                label = { Text(stringResource(R.string.settings_owner_label)) },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    Text(
                        when (error) {
                            OwnerError.EMPTY -> stringResource(R.string.owner_error_empty)
                            OwnerError.TOO_LONG -> stringResource(R.string.owner_error_too_long)
                            null -> stringResource(R.string.settings_owner_current, state.storedOwner)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onSave) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    RasporedTheme {
        SettingsScreen(
            state = SettingsUiState(
                storedOwner = "Јована",
                editingText = "Јована",
                error = null
            ),
            onOwnerTextChanged = {},
            onSave = {},
            onBack = {}
        )
    }
}
