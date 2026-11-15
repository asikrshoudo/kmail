package com.kmail.thekami.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.data.prefs.SignatureState
import com.kmail.thekami.data.prefs.SignatureStore
import com.kmail.thekami.domain.model.Account
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SignatureViewModel @Inject constructor(private val store: SignatureStore) : ViewModel() {
    val state: StateFlow<SignatureState> = store.state
    fun setEnabled(on: Boolean) = store.setEnabled(on)
    fun setDefault(text: String) = store.setDefault(text)
    fun setForAccount(accountId: String, text: String) = store.setForAccount(accountId, text)
}

private class SignatureTarget(val accountId: String?, val title: String, val initial: String)

/** Emits its rows into the caller's Column. */
@Composable
fun SignatureSection(accounts: List<Account>, vm: SignatureViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SignatureTarget?>(null) }
    val defaultTitle = stringResource(R.string.settings_signature_default)

    Text(
        stringResource(R.string.settings_compose),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.settings_signature_enabled),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = state.enabled, onCheckedChange = vm::setEnabled)
    }
    if (state.enabled) {
        SignatureRow(defaultTitle, state.defaultText) {
            editing = SignatureTarget(null, defaultTitle, state.defaultText)
        }
        accounts.forEach { account ->
            val title = stringResource(R.string.settings_signature_for, account.emailAddress)
            val own = state.perAccount[account.id].orEmpty()
            SignatureRow(title, own.ifEmpty { stringResource(R.string.settings_signature_uses_default) }) {
                editing = SignatureTarget(account.id, title, own)
            }
        }
    }
    HorizontalDivider()

    editing?.let { target ->
        var text by remember(target) { mutableStateOf(target.initial) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(target.title) },
            text = {
                Column {
                    OutlinedTextField(value = text, onValueChange = { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    if (target.accountId != null) {
                        Text(
                            stringResource(R.string.settings_signature_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target.accountId == null) vm.setDefault(text) else vm.setForAccount(target.accountId, text)
                    editing = null
                }) { Text(stringResource(R.string.settings_pin_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(R.string.compose_discard_keep)) }
            },
        )
    }
}

@Composable
private fun SignatureRow(title: String, preview: String, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                preview.ifBlank { "\u2014" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onEdit) { Text(stringResource(R.string.settings_signature_edit)) }
    }
}
