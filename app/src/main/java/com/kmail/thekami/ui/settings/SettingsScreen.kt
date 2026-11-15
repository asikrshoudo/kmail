package com.kmail.thekami.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import com.kmail.thekami.ui.theme.ThemeMode
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.data.security.PinVerify
import com.kmail.thekami.domain.model.Account
import kotlinx.coroutines.launch

private sealed interface SettingsDialog {
    data object SetPin : SettingsDialog
    data object DisableLock : SettingsDialog
    data object Recovery : SettingsDialog
    data class Remove(val account: Account) : SettingsDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onNoAccounts: () -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val lock by vm.lock.collectAsStateWithLifecycle()
    val appearance by vm.appearance.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accounts) {
        if (accounts?.isEmpty() == true) onNoAccounts()
    }

    val errWrong = stringResource(R.string.lock_wrong)
    val errWait = stringResource(R.string.lock_wait, 0)
    fun verifyMessage(r: PinVerify, waitFormat: (Int) -> String): String? = when (r) {
        PinVerify.Ok -> null
        PinVerify.Wrong -> errWrong
        is PinVerify.Locked -> waitFormat(r.seconds.toInt())
    }
    val waitFormat: (Int) -> String = { seconds -> errWait.replace("0", seconds.toString()) }
    val errFormat = stringResource(R.string.settings_pin_error_format)
    val errWeak = stringResource(R.string.settings_pin_error_weak)
    val errMismatch = stringResource(R.string.settings_pin_error_mismatch)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_accounts))
            accounts.orEmpty().forEach { account ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(account.emailAddress, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { dialogError = null; dialog = SettingsDialog.Remove(account) }) {
                        Text(stringResource(R.string.settings_remove_account))
                    }
                }
            }
            TextButton(onClick = onAddAccount) { Text(stringResource(R.string.home_add_account)) }
            HorizontalDivider()

            SignatureSection(accounts.orEmpty())

            SectionTitle(stringResource(R.string.settings_appearance))
            Text(stringResource(R.string.appearance_theme), style = MaterialTheme.typography.bodyLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.System to R.string.appearance_system,
                    ThemeMode.Light to R.string.appearance_light,
                    ThemeMode.Dark to R.string.appearance_dark,
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = appearance.mode == mode,
                        onClick = { vm.setThemeMode(mode) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            SwitchRow(R.string.appearance_amoled, R.string.appearance_amoled_desc, appearance.amoled, vm::setAmoled)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(R.string.appearance_dynamic, R.string.appearance_dynamic_desc, appearance.dynamicColor, vm::setDynamicColor)
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_sync))
            SwitchRow(R.string.settings_bg_sync, R.string.settings_bg_sync_desc, sync.backgroundSync, vm::setBackgroundSync)
            if (sync.backgroundSync) {
                Text(stringResource(R.string.settings_interval), style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { minutes ->
                        FilterChip(
                            selected = sync.intervalMinutes == minutes,
                            onClick = { vm.setInterval(minutes) },
                            label = { Text(stringResource(R.string.settings_interval_minutes, minutes)) },
                        )
                    }
                }
            }
            SwitchRow(R.string.settings_notifications, null, sync.notifications, vm::setNotifications)
            if (sync.notifications) {
                SwitchRow(R.string.settings_hide_content, null, sync.hideContent, vm::setHideContent)
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_privacy))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_app_lock), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_app_lock_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = lock.enabled,
                    onCheckedChange = { on ->
                        dialogError = null
                        dialog = if (on) SettingsDialog.SetPin else SettingsDialog.DisableLock
                    },
                )
            }
            if (lock.enabled) {
                if (lock.biometricAvailable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_biometric), modifier = Modifier.weight(1f))
                        Switch(checked = lock.biometric, onCheckedChange = vm::setBiometric)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_recovery), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(if (lock.hasRecovery) R.string.settings_recovery_on else R.string.settings_recovery_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { dialogError = null; dialog = SettingsDialog.Recovery }) {
                        Text(stringResource(if (lock.hasRecovery) R.string.settings_recovery_change else R.string.settings_recovery_setup))
                    }
                }
                Text(stringResource(R.string.settings_auto_lock), style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 60, 300, 900).forEach { seconds ->
                        FilterChip(
                            selected = lock.autoLockSeconds == seconds,
                            onClick = { vm.setAutoLock(seconds) },
                            label = {
                                Text(
                                    if (seconds == 0) stringResource(R.string.settings_auto_lock_immediate)
                                    else stringResource(R.string.settings_auto_lock_minutes, seconds / 60),
                                )
                            },
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_lock_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_about))
            TextButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.settings_privacy_url))),
                        )
                    } catch (e: ActivityNotFoundException) {
                        // no browser installed
                    }
                },
            ) { Text(stringResource(R.string.settings_privacy_policy)) }

            // Only the brand and the version, centered, at the very bottom.
            Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.settings_brand), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_version, vm.versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    when (val d = dialog) {
        SettingsDialog.SetPin -> PinDialog(
            title = stringResource(R.string.settings_pin_set_title),
            message = stringResource(R.string.settings_pin_hint),
            labels = listOf(stringResource(R.string.settings_pin_new), stringResource(R.string.settings_pin_confirm)),
            confirmLabel = stringResource(R.string.settings_pin_save),
            error = dialogError,
            onDismiss = { dialog = null },
            onConfirm = { v ->
                scope.launch {
                    when (vm.enableLock(v[0], v[1])) {
                        PinSetResult.OK -> dialog = SettingsDialog.Recovery
                        PinSetResult.FORMAT -> dialogError = errFormat
                        PinSetResult.WEAK -> dialogError = errWeak
                        PinSetResult.MISMATCH -> dialogError = errMismatch
                    }
                }
            },
        )
        SettingsDialog.DisableLock -> PinDialog(
            title = stringResource(R.string.settings_disable_lock_title),
            message = stringResource(R.string.settings_pin_enter_to_continue),
            labels = listOf(stringResource(R.string.settings_pin_enter)),
            confirmLabel = stringResource(R.string.settings_turn_off),
            error = dialogError,
            onDismiss = { dialog = null },
            onConfirm = { v ->
                scope.launch {
                    val r = vm.disableLock(v[0])
                    if (r is PinVerify.Ok) dialog = null else dialogError = verifyMessage(r, waitFormat)
                }
            },
        )
        is SettingsDialog.Remove -> {
            val title = stringResource(R.string.settings_remove_title, d.account.emailAddress)
            val message = stringResource(R.string.settings_remove_message)
            if (lock.enabled) {
                PinDialog(
                    title = title,
                    message = message + "\n\n" + stringResource(R.string.settings_pin_enter_to_continue),
                    labels = listOf(stringResource(R.string.settings_pin_enter)),
                    confirmLabel = stringResource(R.string.settings_remove_confirm),
                    error = dialogError,
                    onDismiss = { dialog = null },
                    onConfirm = { v ->
                        scope.launch {
                            val r = vm.removeAccount(d.account.id, v[0])
                            if (r is PinVerify.Ok) dialog = null else dialogError = verifyMessage(r, waitFormat)
                        }
                    },
                )
            } else {
                AlertDialog(
                    onDismissRequest = { dialog = null },
                    title = { Text(title) },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                vm.removeAccount(d.account.id, null)
                                dialog = null
                            }
                        }) { Text(stringResource(R.string.settings_remove_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.compose_discard_keep)) }
                    },
                )
            }
        }
        SettingsDialog.Recovery -> RecoverySetupDialog(
            onSave = { q1, a1, q2, a2 -> vm.setRecovery(q1, a1, q2, a2) },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun PinDialog(
    title: String,
    message: String,
    labels: List<String>,
    confirmLabel: String,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    // Plain remember on purpose: a PIN must never end up in saved instance state.
    val values = remember { mutableStateListOf<String>().apply { repeat(labels.size) { add("") } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                labels.forEachIndexed { i, label ->
                    OutlinedTextField(
                        value = values[i],
                        onValueChange = { values[i] = it.filter(Char::isDigit).take(8) },
                        label = { Text(label) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = error != null,
                    )
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values.toList()) }, enabled = values.all { it.length >= 4 }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_discard_keep)) } },
    )
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    @StringRes description: Int?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
