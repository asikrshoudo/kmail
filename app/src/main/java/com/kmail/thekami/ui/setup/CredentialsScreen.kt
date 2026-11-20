package com.kmail.thekami.ui.setup

import com.kmail.thekami.ui.components.AppIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.AuthLayout
import com.kmail.thekami.ui.components.AuthPrimaryButton
import com.kmail.thekami.ui.components.AuthTextField
import com.kmail.thekami.ui.components.HOW_TO_USE_URL
import com.kmail.thekami.ui.components.openLinkAction

@Composable
fun CredentialsScreen(
    state: CredentialsUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContinue: () -> Unit,
    onScanQr: () -> Unit,
    onImportFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showOtherWays by rememberSaveable { mutableStateOf(false) }
    val openLink = openLinkAction()

    AuthLayout(
        title = stringResource(R.string.credentials_title),
        subtitle = stringResource(R.string.credentials_subtitle),
        modifier = modifier,
        actions = {
            IconButton(onClick = { showHelp = true }) {
                Icon(
                    imageVector = AppIcons.Info,
                    contentDescription = stringResource(R.string.credentials_help_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        bottomBar = {
            AuthPrimaryButton(
                text = stringResource(R.string.credentials_continue),
                onClick = onContinue,
            )
            TextButton(
                onClick = { showOtherWays = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.login_other_ways),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) {
        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.credentials_email_label),
            icon = AppIcons.Mail,
            errorText = if (state.emailError) stringResource(R.string.credentials_email_error) else null,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )
        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            isError = state.passwordError,
            imeAction = ImeAction.Done,
            onDone = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.credentials_help_title)) },
            text = { Text(stringResource(R.string.credentials_help_body)) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.credentials_help_close))
                }
            },
            dismissButton = {
                TextButton(onClick = { openLink(HOW_TO_USE_URL) }) {
                    Text(stringResource(R.string.legal_how_to_use))
                }
            },
        )
    }

    if (showOtherWays) {
        OtherWaysDialog(
            onDismiss = { showOtherWays = false },
            onScanQr = {
                showOtherWays = false
                onScanQr()
            },
            onImportFile = {
                showOtherWays = false
                onImportFile()
            },
        )
    }
}

@Composable
private fun OtherWaysDialog(
    onDismiss: () -> Unit,
    onScanQr: () -> Unit,
    onImportFile: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.login_other_ways)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.login_other_ways_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                WayRow(AppIcons.QrScan, stringResource(R.string.credentials_scan_qr), onScanQr)
                WayRow(AppIcons.FolderOpen, stringResource(R.string.credentials_import_file), onImportFile)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.credentials_help_close))
            }
        },
    )
}

@Composable
private fun WayRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
