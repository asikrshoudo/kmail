package com.kmail.thekami.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerFormError
import com.kmail.thekami.ui.components.AuthLayout
import com.kmail.thekami.ui.components.AuthPrimaryButton
import com.kmail.thekami.ui.components.AuthTextField
import com.kmail.thekami.ui.components.NoteBanner
import com.kmail.thekami.ui.components.authFieldColors

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
    AuthLayout(
        title = title,
        overline = protocol,
        modifier = modifier,
        bottomBar = { AuthPrimaryButton(text = continueLabel, onClick = onContinue) },
    ) {
        if (hint != null) NoteBanner(hint)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AuthTextField(
                value = state.form.host,
                onValueChange = onHostChange,
                label = stringResource(R.string.server_host_label),
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Dns,
                errorText = if (ServerFormError.HOST_INVALID in state.errors) {
                    stringResource(R.string.server_host_error)
                } else {
                    null
                },
                keyboardType = KeyboardType.Uri,
            )
            AuthTextField(
                value = state.form.port,
                onValueChange = onPortChange,
                label = stringResource(R.string.server_port_label),
                modifier = Modifier.width(104.dp),
                errorText = if (ServerFormError.PORT_INVALID in state.errors) {
                    stringResource(R.string.server_port_error)
                } else {
                    null
                },
                keyboardType = KeyboardType.Number,
            )
        }

        Text(
            text = stringResource(R.string.server_security_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecurityChip(
                selected = state.form.security == ConnectionSecurity.SSL_TLS,
                label = stringResource(R.string.server_security_tls),
                onClick = { onSecurityChange(ConnectionSecurity.SSL_TLS) },
            )
            SecurityChip(
                selected = state.form.security == ConnectionSecurity.STARTTLS,
                label = stringResource(R.string.server_security_starttls),
                onClick = { onSecurityChange(ConnectionSecurity.STARTTLS) },
            )
        }

        TextField(
            value = stringResource(R.string.server_auth_normal),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_auth_label)) },
            readOnly = true,
            enabled = false,
            singleLine = true,
            colors = authFieldColors(),
        )

        AuthTextField(
            value = state.form.username,
            onValueChange = onUsernameChange,
            label = stringResource(R.string.server_username_label),
            icon = Icons.Outlined.Person,
            errorText = if (ServerFormError.USERNAME_EMPTY in state.errors) {
                stringResource(R.string.server_username_error)
            } else {
                null
            },
            keyboardType = KeyboardType.Email,
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
}

@Composable
private fun SecurityChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}
