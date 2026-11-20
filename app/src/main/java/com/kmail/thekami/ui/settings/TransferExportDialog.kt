package com.kmail.thekami.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.data.security.PinVerify
import com.kmail.thekami.domain.model.Account
import kotlinx.coroutines.launch

private const val MIN_PASSPHRASE = 10

/** Lets the user pick accounts, asks for the PIN (if App Lock is on) and a passphrase, then shows the result. */
@Composable
fun TransferExportDialog(
    accounts: List<Account>,
    preselected: String?,
    onDismiss: () -> Unit,
    vm: TransferExportViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { vm.reset() } }
    val ready = state as? ExportUiState.Ready
    if (ready != null) {
        TransferResultDialog(data = ready.data, onDismiss = onDismiss)
    } else {
        TransferFormDialog(
            accounts = accounts,
            preselected = preselected,
            vm = vm,
            working = state is ExportUiState.Working,
            failed = state is ExportUiState.Failed,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TransferFormDialog(
    accounts: List<Account>,
    preselected: String?,
    vm: TransferExportViewModel,
    working: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val needsPin = remember { vm.lockEnabled }
    var selected by remember {
        mutableStateOf(if (preselected != null) setOf(preselected) else accounts.map { it.id }.toSet())
    }
    // Plain remember on purpose: a PIN or passphrase must never reach saved instance state.
    var pin by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val ids = accounts.map { it.id }.filter { it in selected }
        when {
            ids.isEmpty() -> error = context.getString(R.string.transfer_none_selected)
            passphrase.length < MIN_PASSPHRASE -> error = context.getString(R.string.transfer_passphrase_short)
            passphrase != repeat -> error = context.getString(R.string.transfer_passphrase_mismatch)
            else -> scope.launch {
                if (needsPin) {
                    val check = vm.verifyPin(pin)
                    if (check != PinVerify.Ok) {
                        error = when (check) {
                            is PinVerify.Locked -> context.getString(R.string.lock_wait, check.seconds.toInt())
                            else -> context.getString(R.string.lock_wrong)
                        }
                        return@launch
                    }
                }
                error = null
                vm.prepare(ids, passphrase)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(stringResource(R.string.transfer_title)) },
        text = {
            SecureWindow()
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.transfer_intro), style = MaterialTheme.typography.bodyMedium)
                if (accounts.size > 1) {
                    Text(stringResource(R.string.transfer_choose), style = MaterialTheme.typography.labelLarge)
                    accounts.forEach { account ->
                        val checked = account.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !working) {
                                    selected = if (checked) selected - account.id else selected + account.id
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(account.emailAddress, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (needsPin) {
                    PinField(pin, { pin = it.filter(Char::isDigit).take(8) }, stringResource(R.string.settings_pin_enter))
                }
                PassphraseField(passphrase, { passphrase = it }, stringResource(R.string.transfer_passphrase))
                PassphraseField(
                    value = repeat,
                    onValueChange = { repeat = it },
                    label = stringResource(R.string.transfer_passphrase_repeat),
                    imeAction = ImeAction.Done,
                    onDone = { if (!working) submit() },
                )
                val message = error ?: if (failed) stringResource(R.string.transfer_failed) else null
                if (message != null) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (working) Text(stringResource(R.string.transfer_working), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = !working) { Text(stringResource(R.string.transfer_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

@Composable
private fun PinField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}
