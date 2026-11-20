package com.kmail.thekami.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.AuthCard
import com.kmail.thekami.ui.components.AuthLayout
import com.kmail.thekami.ui.components.AuthPrimaryButton
import com.kmail.thekami.ui.components.AuthSecondaryButton
import com.kmail.thekami.ui.components.AuthTextField

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
    val helpDescription = stringResource(R.string.credentials_help_description)

    AuthLayout(
        icon = Icons.Rounded.AlternateEmail,
        title = stringResource(R.string.credentials_title),
        subtitle = stringResource(R.string.credentials_subtitle),
        modifier = modifier,
        actions = {
            OutlinedIconButton(
                onClick = { showHelp = true },
                modifier = Modifier.semantics { contentDescription = helpDescription },
            ) {
                Text("?", modifier = Modifier.clearAndSetSemantics { })
            }
        },
    ) {
        AuthCard {
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.credentials_email_label),
                icon = Icons.Rounded.AlternateEmail,
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
            AuthPrimaryButton(
                text = stringResource(R.string.credentials_continue),
                onClick = onContinue,
            )
        }

        Text(
            text = stringResource(R.string.welcome_other_device),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthSecondaryButton(
                icon = Icons.Rounded.QrCodeScanner,
                text = stringResource(R.string.credentials_scan_qr),
                onClick = onScanQr,
                modifier = Modifier.weight(1f),
            )
            AuthSecondaryButton(
                icon = Icons.Rounded.FolderOpen,
                text = stringResource(R.string.credentials_import_file),
                onClick = onImportFile,
                modifier = Modifier.weight(1f),
            )
        }
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
        )
    }
}
