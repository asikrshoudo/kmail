package com.kmail.thekami.ui.compose

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.ui.setup.messageRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    vm: ComposeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDiscard by remember { mutableStateOf(false) }
    var fromMenu by remember { mutableStateOf(false) }
    var body by remember { mutableStateOf(TextFieldValue()) }
    val sentMessage = stringResource(R.string.compose_sent)

    // Put the cursor at the top of the prefilled body, above signature and quote.
    LaunchedEffect(state.loading) {
        if (!state.loading) body = TextFieldValue(state.body, TextRange.Zero)
    }
    LaunchedEffect(state.sent) {
        if (state.sent) {
            Toast.makeText(context, sentMessage, Toast.LENGTH_SHORT).show()
            onClose()
        }
    }
    val attachLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }
    val requestClose = { if (state.dirty && !state.sending) showDiscard = true else onClose() }
    BackHandler(enabled = state.dirty && !state.sending) { showDiscard = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (vm.mode) {
                                "reply", "replyall" -> R.string.compose_reply
                                "forward" -> R.string.compose_forward
                                else -> R.string.compose_new
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                },
                actions = {
                    TextButton(onClick = vm::send, enabled = !state.sending && !state.loading) {
                        Text(stringResource(R.string.compose_send))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.sending) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.compose_sending), style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { error ->
                val text = when (error) {
                    ComposeError.NoRecipient -> stringResource(R.string.compose_error_no_recipient)
                    is ComposeError.InvalidRecipient -> stringResource(R.string.compose_error_invalid_recipient, error.text)
                    ComposeError.AttachmentTooLarge -> stringResource(R.string.compose_error_too_large)
                    is ComposeError.SendFailed -> error.error?.let {
                        stringResource(R.string.compose_error_send, stringResource(it.messageRes()))
                    } ?: stringResource(R.string.compose_error_no_account)
                }
                Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.compose_from), style = MaterialTheme.typography.labelLarge)
                Box {
                    val selected = state.accounts.firstOrNull { it.id == state.fromId }
                    TextButton(onClick = { fromMenu = true }, enabled = state.accounts.size > 1) {
                        Text(selected?.emailAddress.orEmpty())
                    }
                    DropdownMenu(expanded = fromMenu, onDismissRequest = { fromMenu = false }) {
                        state.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.emailAddress) },
                                onClick = {
                                    fromMenu = false
                                    vm.onFrom(account.id)
                                },
                            )
                        }
                    }
                }
            }

            RecipientField(R.string.compose_to, state.to, vm::onTo) {
                if (!state.showCcBcc) {
                    TextButton(onClick = vm::showCcBcc) { Text(stringResource(R.string.compose_show_cc)) }
                }
            }
            if (state.showCcBcc) {
                RecipientField(R.string.compose_cc, state.cc, vm::onCc)
                RecipientField(R.string.compose_bcc, state.bcc, vm::onBcc)
            }

            OutlinedTextField(
                value = state.subject,
                onValueChange = vm::onSubject,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.compose_subject)) },
                singleLine = true,
            )

            if (state.attachments.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Wrapped in a column scroll above; chips stay on one row and scroll with the page.
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.attachments.forEach { attachment ->
                        AssistChip(
                            onClick = { vm.removeAttachment(attachment) },
                            label = {
                                Text(
                                    stringResource(R.string.compose_remove_attachment, attachment.name),
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            }
            TextButton(onClick = { attachLauncher.launch(arrayOf("*/*")) }) {
                Text(stringResource(R.string.compose_attach))
            }

            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    vm.onBody(it.text)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.compose_body_hint)) },
                minLines = 10,
            )
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.compose_discard_title)) },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; onClose() }) {
                    Text(stringResource(R.string.compose_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.compose_discard_keep))
                }
            },
        )
    }
}

@Composable
private fun RecipientField(
    labelRes: Int,
    value: String,
    onChange: (String) -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(labelRes)) },
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email,
        ),
    )
}
