package com.kmail.thekami.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.domain.model.ConnectionSecurity
import com.kmail.thekami.domain.model.ServerFormError
import com.kmail.thekami.ui.components.AuthCard
import com.kmail.thekami.ui.components.AuthLayout
import com.kmail.thekami.ui.components.AuthPrimaryButton
import com.kmail.thekami.ui.components.AuthTextField
import com.kmail.thekami.ui.components.NoteBanner
import com.kmail.thekami.ui.components.ProtocolPill

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
        icon = Icons.Rounded.Dns,
        title = title,
        modifier = modifier,
        headerExtra = {
            ProtocolPill(protocol)
            if (hint != null) NoteBanner(hint)
        },
    ) {
        AuthCard {
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
                    icon = Icons.Rounded.Dns,
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
                    modifier = Modifier.width(120.dp),
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
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecurityChip(
                    selected = state.form.security == ConnectionSecurity.SSL_TLS,
                    label = stringResource(R.string.server_security_tls),
                    onClick = { onSecurityChange(ConnectionSecurity.SSL_TLS) },
                    modifier = Modifier.weight(1f),
                )
                SecurityChip(
                    selected = state.form.security == ConnectionSecurity.STARTTLS,
                    label = stringResource(R.string.server_security_starttls),
                    onClick = { onSecurityChange(ConnectionSecurity.STARTTLS) },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = stringResource(R.string.server_auth_normal),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.server_auth_label)) },
                shape = RoundedCornerShape(16.dp),
                readOnly = true,
                enabled = false,
                singleLine = true,
            )

            AuthTextField(
                value = state.form.username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.server_username_label),
                icon = Icons.Rounded.Person,
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

        AuthPrimaryButton(text = continueLabel, onClick = onContinue)
    }
}

@Composable
private fun SecurityChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}
