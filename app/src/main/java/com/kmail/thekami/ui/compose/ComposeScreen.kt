package com.kmail.thekami.ui.compose

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.data.contacts.Suggestion
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
    var moreMenu by remember { mutableStateOf(false) }
    var body by remember { mutableStateOf(TextFieldValue()) }
    val sentMessage = stringResource(R.string.compose_sent)
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshSuggestions() }
    val focusField = { kind: RecipientKind ->
        vm.onFocusField(kind)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED && vm.takeContactsPrompt()) {
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

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
    LaunchedEffect(state.closeRequested) { if (state.closeRequested) onClose() }

    val attachLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.addAttachments(uris)
    }
    // Leaving keeps the draft: it is autosaved and listed under "Unsent drafts".
    val leave = {
        vm.leave()
        onClose()
    }
    BackHandler(enabled = !state.sending) { leave() }
    val canSend = !state.sending && !state.loading

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
                                "draft" -> R.string.compose_draft
                                else -> R.string.compose_new
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                },
                actions = {
                    IconButton(onClick = { attachLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = stringResource(R.string.compose_attach))
                    }
                    IconButton(onClick = vm::send, enabled = canSend) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(R.string.compose_send),
                            tint = if (canSend) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                    Box {
                        IconButton(onClick = { moreMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.home_more_options))
                        }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.compose_discard_confirm)) },
                                onClick = {
                                    moreMenu = false
                                    showDiscard = true
                                },
                            )
                        }
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
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.sending) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
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
                Text(
                    text,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // From
            val canSwitch = state.accounts.size > 1
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(enabled = canSwitch) { fromMenu = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldLabel(R.string.compose_from)
                Box(Modifier.weight(1f)) {
                    val selected = state.accounts.firstOrNull { it.id == state.fromId }
                    Text(
                        selected?.emailAddress.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                if (canSwitch) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FieldDivider()

            RecipientRow(R.string.compose_to, state.to, vm::onTo, onFocus = { focusField(RecipientKind.TO) }) {
                if (!state.showCcBcc) {
                    Text(
                        stringResource(R.string.compose_show_cc),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = vm::showCcBcc)
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
            }
            if (state.showCcBcc) {
                RecipientRow(R.string.compose_cc, state.cc, vm::onCc, onFocus = { focusField(RecipientKind.CC) })
                RecipientRow(R.string.compose_bcc, state.bcc, vm::onBcc, onFocus = { focusField(RecipientKind.BCC) })
            }
            SuggestionPanel(state.suggestions, vm::pickSuggestion)

            // Subject
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlatInput(
                    value = state.subject,
                    onChange = vm::onSubject,
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(R.string.compose_subject),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
            FieldDivider()

            // Attachments
            state.attachments.forEach { attachment ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        attachment.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                    IconButton(onClick = { vm.removeAttachment(attachment) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.compose_remove_attachment, attachment.name),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.attachments.isNotEmpty()) FieldDivider()

            // Body: no box, the page itself is the writing surface.
            BasicTextField(
                value = body,
                onValueChange = {
                    body = it
                    vm.onBody(it.text)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 12,
                decorationBox = { inner ->
                    Box {
                        if (body.text.isEmpty()) {
                            Text(
                                stringResource(R.string.compose_body_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.compose_discard_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    vm.discard()
                }) { Text(stringResource(R.string.compose_discard_confirm)) }
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
private fun FieldDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

@Composable
private fun FieldLabel(labelRes: Int) {
    Text(
        stringResource(labelRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        modifier = Modifier.width(72.dp),
    )
}

/** A borderless single field: just text on the page. */
@Composable
private fun FlatInput(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        decorationBox = { inner ->
            Box(Modifier.padding(vertical = 12.dp)) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun RecipientRow(
    labelRes: Int,
    value: String,
    onChange: (String) -> Unit,
    onFocus: () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldLabel(labelRes)
        FlatInput(
            value = value,
            onChange = onChange,
            modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFocus() },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
            ),
        )
        trailing()
    }
    FieldDivider()
}

@Composable
private fun SuggestionPanel(suggestions: List<Suggestion>, onPick: (Suggestion) -> Unit) {
    if (suggestions.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        suggestions.forEach { s ->
            Column(
                Modifier.fillMaxWidth().clickable { onPick(s) }.padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(s.name ?: s.address, style = MaterialTheme.typography.bodyMedium)
                if (s.name != null) {
                    Text(
                        s.address,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FieldDivider()
        }
    }
}
