package com.kmail.thekami.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerFormError

@Composable
fun ServerScreen(
    title: String,
    protocol: String,
    hint: String?,
    state: ServerUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSecurityChange: (ConnectionSecurity) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    continueLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = protocol,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = state.form.host,
            onValueChange = onHostChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_host_label)) },
            singleLine = true,
            isError = ServerFormError.HOST_INVALID in state.errors,
            supportingText = {
                if (ServerFormError.HOST_INVALID in state.errors) {
                    Text(stringResource(R.string.server_host_error))
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
        )

        OutlinedTextField(
            value = state.form.port,
            onValueChange = onPortChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_port_label)) },
            singleLine = true,
            isError = ServerFormError.PORT_INVALID in state.errors,
            supportingText = {
                if (ServerFormError.PORT_INVALID in state.errors) {
                    Text(stringResource(R.string.server_port_error))
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )

        Text(
            text = stringResource(R.string.server_security_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.form.security == ConnectionSecurity.SSL_TLS,
                onClick = { onSecurityChange(ConnectionSecurity.SSL_TLS) },
                label = { Text(stringResource(R.string.server_security_tls)) },
            )
            FilterChip(
                selected = state.form.security == ConnectionSecurity.STARTTLS,
                onClick = { onSecurityChange(ConnectionSecurity.STARTTLS) },
                label = { Text(stringResource(R.string.server_security_starttls)) },
            )
        }

        OutlinedTextField(
            value = stringResource(R.string.server_auth_normal),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_auth_label)) },
            readOnly = true,
            enabled = false,
            singleLine = true,
        )

        OutlinedTextField(
            value = state.form.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_username_label)) },
            singleLine = true,
            isError = ServerFormError.USERNAME_EMPTY in state.errors,
            supportingText = {
                if (ServerFormError.USERNAME_EMPTY in state.errors) {
                    Text(stringResource(R.string.server_username_error))
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )

        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            isError = state.passwordError,
            imeAction = ImeAction.Done,
            onDone = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(continueLabel)
        }
    }
}
