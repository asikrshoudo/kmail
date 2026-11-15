package com.kmail.thekami.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.data.security.PinChange
import com.kmail.thekami.data.security.PinVerify
import kotlinx.coroutines.launch

class RecoveryActions(
    val questions: List<String>,
    val verify: suspend (List<String>) -> PinVerify,
    val setNewPin: suspend (String, String) -> PinChange,
    val onDone: () -> Unit,
)

/** Step 1: answer the questions. Step 2: choose a new PIN. Nothing here is kept in saved state. */
@Composable
fun RecoveryFlow(actions: RecoveryActions, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var verified by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<PinVerify?>(null) }
    var pinResult by remember { mutableStateOf<PinChange?>(null) }
    var busy by remember { mutableStateOf(false) }
    val answers = remember { mutableStateListOf<String>().apply { repeat(actions.questions.size) { add("") } } }
    val pins = remember { mutableStateListOf("", "") }

    if (!verified) {
        val error = when (val r = verifyResult) {
            PinVerify.Wrong -> stringResource(R.string.recovery_wrong)
            is PinVerify.Locked -> stringResource(R.string.lock_wait, r.seconds.toInt())
            else -> null
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.recovery_title)) },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.recovery_answer_hint), style = MaterialTheme.typography.bodySmall)
                    actions.questions.forEachIndexed { i, question ->
                        Text(question, style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = answers[i],
                            onValueChange = { answers[i] = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = error != null,
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                        )
                    }
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && answers.all { it.isNotBlank() },
                    onClick = {
                        scope.launch {
                            busy = true
                            val r = actions.verify(answers.toList())
                            if (r is PinVerify.Ok) verified = true else verifyResult = r
                            busy = false
                        }
                    },
                ) { Text(stringResource(R.string.recovery_continue)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_discard_keep)) } },
        )
    } else {
        val error = when (pinResult) {
            PinChange.FORMAT -> stringResource(R.string.settings_pin_error_format)
            PinChange.WEAK -> stringResource(R.string.settings_pin_error_weak)
            PinChange.MISMATCH -> stringResource(R.string.settings_pin_error_mismatch)
            else -> null
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.recovery_new_pin_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_pin_hint), style = MaterialTheme.typography.bodySmall)
                    listOf(R.string.settings_pin_new, R.string.settings_pin_confirm).forEachIndexed { i, label ->
                        OutlinedTextField(
                            value = pins[i],
                            onValueChange = { pins[i] = it.filter(Char::isDigit).take(8) },
                            label = { Text(stringResource(label)) },
                            singleLine = true,
                            isError = error != null,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        )
                    }
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && pins.all { it.length >= 4 },
                    onClick = {
                        scope.launch {
                            busy = true
                            val r = actions.setNewPin(pins[0], pins[1])
                            if (r == PinChange.OK) actions.onDone() else pinResult = r
                            busy = false
                        }
                    },
                ) { Text(stringResource(R.string.settings_pin_save)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_discard_keep)) } },
        )
    }
}
