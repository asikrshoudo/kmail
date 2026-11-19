package com.kmail.thekami.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.ui.settings.SecureWindow

/** Asks for the passphrase of a scanned or picked transfer, then hands it to [setupVm]. */
@Composable
fun TransferImportHost(
    importVm: TransferImportViewModel,
    setupVm: SetupViewModel,
    onReady: () -> Unit,
) {
    val state by importVm.state.collectAsStateWithLifecycle()
    val current = state
    if (current is ImportUiState.AskPassphrase || current is ImportUiState.Working) {
        PassphraseDialog(
            error = (current as? ImportUiState.AskPassphrase)?.error,
            working = current is ImportUiState.Working,
            onSubmit = importVm::unlock,
            onCancel = importVm::clear,
        )
    } else if (current is ImportUiState.Rejected) {
        AlertDialog(
            onDismissRequest = importVm::clear,
            title = { Text(stringResource(R.string.import_rejected_title)) },
            text = { Text(stringResource(R.string.import_rejected_body)) },
            confirmButton = { TextButton(onClick = importVm::clear) { Text(stringResource(R.string.import_ok)) } },
        )
    }
    LaunchedEffect(current) {
        if (current is ImportUiState.Unlocked) {
            setupVm.loadTransfer(current.data)
            current.data.wipe()
            importVm.clear()
            onReady()
        }
    }
}

@Composable
private fun PassphraseDialog(
    error: ImportError?,
    working: Boolean,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    // Plain remember on purpose: a passphrase must never reach saved instance state.
    var passphrase by remember { mutableStateOf("") }
    val errorText = when (error) {
        ImportError.WRONG_OR_DAMAGED -> stringResource(R.string.import_wrong)
        ImportError.UNSUPPORTED -> stringResource(R.string.import_unsupported)
        null -> null
    }
    AlertDialog(
        onDismissRequest = { if (!working) onCancel() },
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            SecureWindow()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.import_intro), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.transfer_passphrase)) },
                    singleLine = true,
                    isError = errorText != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (passphrase.isNotEmpty() && !working) onSubmit(passphrase) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorText != null) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (working) Text(stringResource(R.string.transfer_working), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(passphrase) }, enabled = !working && passphrase.isNotEmpty()) {
                Text(stringResource(R.string.import_unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !working) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}
