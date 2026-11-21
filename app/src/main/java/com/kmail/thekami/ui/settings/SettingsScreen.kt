package com.kmail.thekami.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.data.security.PinVerify
import com.kmail.thekami.domain.model.Account
import com.kmail.thekami.ui.components.AppIcons
import com.kmail.thekami.ui.components.HOW_TO_USE_URL
import com.kmail.thekami.ui.components.PRIVACY_URL
import com.kmail.thekami.ui.components.openLinkAction
import com.kmail.thekami.ui.support.SupportSection
import com.kmail.thekami.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private enum class SettingsPage { Hub, Accounts, Signature, Appearance, Language, Sync, Privacy, About }

private sealed interface SettingsDialog {
    data object SetPin : SettingsDialog
    data object DisableLock : SettingsDialog
    data object Recovery : SettingsDialog
    data class Remove(val account: Account) : SettingsDialog
    data class Transfer(val preselected: String?) : SettingsDialog
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
    val openLink = openLinkAction()
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.Hub.name) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }

    val page = SettingsPage.valueOf(pageName)
    val detail = detailId?.let { id -> accounts.orEmpty().firstOrNull { it.id == id } }

    LaunchedEffect(accounts) {
        if (accounts?.isEmpty() == true) onNoAccounts()
        if (detailId != null && accounts?.none { it.id == detailId } == true) detailId = null
    }

    fun goBack() {
        when {
            detailId != null -> detailId = null
            page != SettingsPage.Hub -> pageName = SettingsPage.Hub.name
            else -> onBack()
        }
    }
    BackHandler(enabled = page != SettingsPage.Hub || detailId != null) { goBack() }

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

    val title = if (detail != null) {
        detail.emailAddress
    } else {
        stringResource(
            when (page) {
                SettingsPage.Hub -> R.string.settings_title
                SettingsPage.Accounts -> R.string.settings_accounts
                SettingsPage.Signature -> R.string.settings_signature_title
                SettingsPage.Appearance -> R.string.settings_appearance
                SettingsPage.Language -> R.string.settings_language
                SettingsPage.Sync -> R.string.settings_sync
                SettingsPage.Privacy -> R.string.settings_privacy
                SettingsPage.About -> R.string.settings_about
            },
        )
    }
    val themeSummary = stringResource(
        when (appearance.mode) {
            ThemeMode.Light -> R.string.appearance_light
            ThemeMode.Dark -> R.string.appearance_dark
            else -> R.string.appearance_system
        },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(AppIcons.Back, contentDescription = stringResource(R.string.reader_back))
                    }
                },
            )
        },
    ) { padding ->
        Crossfade(
            targetState = if (detailId != null) "detail" else pageName,
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "settings-page",
        ) { key ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (key) {
                    "detail" -> if (detail != null) {
                        AccountDetailPage(detail, onTransfer = { dialog = SettingsDialog.Transfer(detail.id) }) { dialogError = null; dialog = SettingsDialog.Remove(detail) }
                    }

                    SettingsPage.Accounts.name -> {
                        accounts.orEmpty().forEach { account ->
                            AccountRow(account) { detailId = account.id }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onAddAccount)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(AppIcons.AddAccount, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(R.string.home_add_account),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        NavRow(AppIcons.QrCode, stringResource(R.string.transfer_title), stringResource(R.string.transfer_row_subtitle)) { dialog = SettingsDialog.Transfer(null) }
                    }

                    SettingsPage.Signature.name -> {
                        SignatureSection(accounts.orEmpty())
                    }

                    SettingsPage.Appearance.name -> {
                        ChipGroup(R.string.appearance_theme) {
                            ChipRow(
                                options = listOf(
                                    ThemeMode.System to stringResource(R.string.appearance_system),
                                    ThemeMode.Light to stringResource(R.string.appearance_light),
                                    ThemeMode.Dark to stringResource(R.string.appearance_dark),
                                ),
                                selected = appearance.mode,
                                onSelect = vm::setThemeMode,
                                equalWidth = true,
                            )
                        }
                        SwitchRow(
                            R.string.appearance_amoled,
                            R.string.appearance_amoled_desc,
                            appearance.amoled,
                            vm::setAmoled,
                            enabled = appearance.mode != ThemeMode.Light,
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            SwitchRow(
                                R.string.appearance_dynamic,
                                R.string.appearance_dynamic_desc,
                                appearance.dynamicColor,
                                vm::setDynamicColor,
                            )
                        }
                    }

                    SettingsPage.Language.name -> {
                        LanguagePage()
                    }

                    SettingsPage.Sync.name -> {
                        SwitchRow(R.string.settings_bg_sync, R.string.settings_bg_sync_desc, sync.backgroundSync, vm::setBackgroundSync)
                        if (sync.backgroundSync) {
                            ChipGroup(R.string.settings_interval) {
                                ChipRow(
                                    options = listOf(5, 10, 15, 30, 60).map {
                                        it to stringResource(R.string.settings_interval_minutes, it)
                                    },
                                    selected = sync.intervalMinutes,
                                    onSelect = vm::setInterval,
                                    equalWidth = true,
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        SwitchRow(R.string.settings_notifications, null, sync.notifications, vm::setNotifications)
                        SwitchRow(
                            R.string.settings_hide_content,
                            null,
                            sync.hideContent,
                            vm::setHideContent,
                            enabled = sync.notifications,
                        )
                    }

                    SettingsPage.Privacy.name -> {
                        SwitchRow(
                            R.string.settings_app_lock,
                            R.string.settings_app_lock_desc,
                            lock.enabled,
                            { on ->
                                dialogError = null
                                dialog = if (on) SettingsDialog.SetPin else SettingsDialog.DisableLock
                            },
                        )
                        if (lock.enabled) {
                            if (lock.biometricAvailable) {
                                SwitchRow(R.string.settings_biometric, null, lock.biometric, vm::setBiometric)
                            }
                            ActionRow(
                                title = stringResource(R.string.settings_recovery),
                                subtitle = stringResource(
                                    if (lock.hasRecovery) R.string.settings_recovery_on else R.string.settings_recovery_off,
                                ),
                                action = stringResource(
                                    if (lock.hasRecovery) R.string.settings_recovery_change else R.string.settings_recovery_setup,
                                ),
                                onAction = { dialogError = null; dialog = SettingsDialog.Recovery },
                            )
                            ChipGroup(R.string.settings_auto_lock) {
                                ChipRow(
                                    options = listOf(0, 60, 300, 900).map { seconds ->
                                        seconds to if (seconds == 0) {
                                            stringResource(R.string.settings_auto_lock_immediate)
                                        } else {
                                            stringResource(R.string.settings_auto_lock_minutes, seconds / 60)
                                        }
                                    },
                                    selected = lock.autoLockSeconds,
                                    onSelect = vm::setAutoLock,
                                )
                            }
                            Text(
                                stringResource(R.string.settings_lock_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                    }

                    SettingsPage.About.name -> {
                        LinkRow(stringResource(R.string.settings_privacy_policy)) { openLink(PRIVACY_URL) }
                        LinkRow(stringResource(R.string.legal_how_to_use)) { openLink(HOW_TO_USE_URL) }
                        VersionFooter(vm.versionName)
                    }

                    else -> {
                        val open = { p: SettingsPage -> pageName = p.name }
                        NavRow(
                            AppIcons.AccountCircle,
                            stringResource(R.string.settings_accounts),
                            accounts.orEmpty().joinToString { it.emailAddress }.ifEmpty { null },
                        ) { open(SettingsPage.Accounts) }
                        NavDivider()
                        NavRow(AppIcons.NoteEdit, stringResource(R.string.settings_signature_title), null) {
                            open(SettingsPage.Signature)
                        }
                        NavDivider()
                        NavRow(AppIcons.Palette, stringResource(R.string.settings_appearance), themeSummary) {
                            open(SettingsPage.Appearance)
                        }
                        NavDivider()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            NavRow(
                                AppIcons.Language,
                                stringResource(R.string.settings_language),
                                currentLanguageLabel(),
                            ) { open(SettingsPage.Language) }
                            NavDivider()
                        }
                        NavRow(AppIcons.Sync, stringResource(R.string.settings_sync), null) { open(SettingsPage.Sync) }
                        NavDivider()
                        NavRow(AppIcons.Lock, stringResource(R.string.settings_privacy), null) { open(SettingsPage.Privacy) }
                        NavDivider()
                        NavRow(AppIcons.Info, stringResource(R.string.settings_about), null) { open(SettingsPage.About) }
                        SupportSection()
                        VersionFooter(vm.versionName)
                    }
                }
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
            val dTitle = stringResource(R.string.settings_remove_title, d.account.emailAddress)
            val dMessage = stringResource(R.string.settings_remove_message)
            if (lock.enabled) {
                PinDialog(
                    title = dTitle,
                    message = dMessage + "\n\n" + stringResource(R.string.settings_pin_enter_to_continue),
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
                    title = { Text(dTitle) },
                    text = { Text(dMessage) },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                vm.removeAccount(d.account.id, null)
                                dialog = null
                            }
                        }) {
                            Text(stringResource(R.string.settings_remove_confirm), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.settings_cancel)) }
                    },
                )
            }
        }
        SettingsDialog.Recovery -> RecoverySetupDialog(
            onSave = { q1, a1, q2, a2 -> vm.setRecovery(q1, a1, q2, a2) },
            onDismiss = { dialog = null },
        )
        is SettingsDialog.Transfer -> TransferExportDialog(accounts = accounts.orEmpty(), preselected = d.preselected, onDismiss = { dialog = null })
        null -> Unit
    }
}

/** Brand name and app version, as small centered text. */
@Composable
private fun VersionFooter(versionName: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.settings_brand), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** A row that opens a web page. */
@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(AppIcons.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Avatar(label: String, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.take(1).uppercase(),
            style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NavDivider() {
    HorizontalDivider(
        Modifier.padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(account.emailAddress, 40.dp)
        Text(
            account.emailAddress,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AccountDetailPage(account: Account, onTransfer: () -> Unit, onRemove: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(top = 32.dp)) { Avatar(account.emailAddress, 72.dp) }
        Text(
            account.emailAddress,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        )
        HorizontalDivider()
        NavRow(AppIcons.QrCode, stringResource(R.string.transfer_title), stringResource(R.string.transfer_row_subtitle), onTransfer)
        NavDivider()
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onRemove).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(AppIcons.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                stringResource(R.string.settings_remove_account),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    @StringRes description: Int?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.38f
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            if (description != null) {
                Text(
                    stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String?, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun ChipGroup(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun <T> ChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    equalWidth: Boolean = false,
) {
    val scroll = rememberScrollState()
    Row(
        Modifier.fillMaxWidth().then(if (equalWidth) Modifier else Modifier.horizontalScroll(scroll)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = if (equalWidth) Modifier.fillMaxWidth() else Modifier,
                    )
                },
                modifier = if (equalWidth) Modifier.weight(1f) else Modifier,
            )
        }
    }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}
