package com.kmail.thekami.ui.lock

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.data.security.PinVerify
import kotlinx.coroutines.launch

/** Covers the whole app and swallows touches. The app underneath keeps its state. */
@Composable
fun LockScreen(
    biometricAvailable: Boolean,
    onVerify: suspend (String) -> PinVerify,
    onBiometric: () -> Unit,
    recovery: RecoveryActions? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<PinVerify?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler { (context as? Activity)?.moveTaskToBack(true) }
    LaunchedEffect(biometricAvailable) { if (biometricAvailable) onBiometric() }

    val submit = {
        if (!busy && pin.length >= 4) {
            scope.launch {
                busy = true
                val r = onVerify(pin)
                result = r
                if (r !is PinVerify.Ok) pin = ""
                busy = false
            }
        }
        Unit
    }
    val error = when (val r = result) {
        PinVerify.Wrong -> stringResource(R.string.lock_wrong)
        is PinVerify.Locked -> stringResource(R.string.lock_wait, r.seconds.toInt())
        else -> null
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.lock_pin_label)) },
                singleLine = true,
                isError = error != null,
                supportingText = { if (error != null) Text(error) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Button(onClick = { submit() }, enabled = pin.length >= 4 && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lock_unlock))
            }
            if (biometricAvailable) {
                TextButton(onClick = onBiometric) { Text(stringResource(R.string.lock_biometric)) }
            }
            if (recovery != null) {
                TextButton(onClick = { showRecovery = true }) { Text(stringResource(R.string.lock_forgot)) }
            }
        }
    }

    if (showRecovery && recovery != null) {
        RecoveryFlow(recovery) { showRecovery = false }
    }
}
