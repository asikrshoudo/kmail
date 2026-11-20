package com.kmail.thekami.ui.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.ui.settings.PassphraseField
import com.kmail.thekami.ui.settings.SecureWindow

/** Shows every step of an import: passphrase, progress and a result for each account. */
@Composable
fun TransferImportHost(importVm: TransferImportViewModel, onOpenInbox: () -> Unit) {
    val state by importVm.state.collectAsStateWithLifecycle()
    val current = state
    if (current is ImportUiState.AskPassphrase || current is ImportUiState.Decrypting) {
        PassphraseDialog(
            error = (current as? ImportUiState.AskPassphrase)?.error,
            working = current is ImportUiState.Decrypting,
            onSubmit = importVm::unlock,
            onCancel = importVm::clear,
        )
    }
    when (current) {
        is ImportUiState.Importing -> ProgressDialog(current)
        is ImportUiState.Finished -> ResultDialog(current.results, onOpenInbox, importVm::clear)
        ImportUiState.Rejected -> AlertDialog(
            onDismissRequest = importVm::clear,
            title = { Text(stringResource(R.string.import_rejected_title)) },
            text = { Text(stringResource(R.string.import_rejected_body)) },
            confirmButton = { TextButton(onClick = importVm::clear) { Text(stringResource(R.string.import_ok)) } },
        )
        else -> Unit
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
                PassphraseField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = stringResource(R.string.transfer_passphrase),
                    isError = errorText != null,
                    imeAction = ImeAction.Done,
                    onDone = { if (passphrase.isNotEmpty() && !working) onSubmit(passphrase) },
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

@Composable
private fun ProgressDialog(state: ImportUiState.Importing) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.import_progress, state.index, state.total, state.email))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ResultDialog(results: List<ImportResult>, onOpenInbox: () -> Unit, onClose: () -> Unit) {
    val anyAdded = results.any { it.outcome == ImportOutcome.ADDED }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.import_result_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                results.forEach { result ->
                    Column {
                        Text(result.email, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(outcomeText(result.outcome)),
                            style = MaterialTheme.typography.bodySmall,
                            color = when (result.outcome) {
                                ImportOutcome.ADDED -> MaterialTheme.colorScheme.primary
                                ImportOutcome.ALREADY_EXISTS -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClose()
                if (anyAdded) onOpenInbox()
            }) {
                Text(stringResource(if (anyAdded) R.string.import_open_inbox else R.string.import_close))
            }
        },
    )
}

@StringRes
private fun outcomeText(outcome: ImportOutcome): Int = when (outcome) {
    ImportOutcome.ADDED -> R.string.import_added
    ImportOutcome.ALREADY_EXISTS -> R.string.import_exists
    ImportOutcome.INCOMING_FAILED -> R.string.import_incoming_failed
    ImportOutcome.OUTGOING_FAILED -> R.string.import_outgoing_failed
    ImportOutcome.FAILED -> R.string.import_failed
}
