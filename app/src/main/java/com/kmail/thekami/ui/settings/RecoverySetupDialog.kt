package com.kmail.thekami.ui.settings

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
import androidx.compose.ui.unit.dp
import com.kmail.thekami.R
import com.kmail.thekami.data.security.RecoveryPolicy
import kotlinx.coroutines.launch

/** Two questions written by the user. Answers are never shown again and are stored only as hashes. */
@Composable
fun RecoverySetupDialog(
    onSave: suspend (String, String, String, String) -> RecoveryPolicy.Problem?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Plain remember on purpose: answers must never end up in saved instance state.
    val fields = remember { mutableStateListOf("", "", "", "") }
    var problem by remember { mutableStateOf<RecoveryPolicy.Problem?>(null) }
    var busy by remember { mutableStateOf(false) }

    val error = when (problem) {
        RecoveryPolicy.Problem.QUESTION_SHORT -> stringResource(R.string.recovery_err_question_short)
        RecoveryPolicy.Problem.ANSWER_SHORT -> stringResource(R.string.recovery_err_answer_short)
        RecoveryPolicy.Problem.ANSWER_COMMON -> stringResource(R.string.recovery_err_answer_common)
        RecoveryPolicy.Problem.ANSWER_REPEATS_QUESTION -> stringResource(R.string.recovery_err_same)
        RecoveryPolicy.Problem.DUPLICATE_QUESTIONS -> stringResource(R.string.recovery_err_dup_questions)
        RecoveryPolicy.Problem.DUPLICATE_ANSWERS -> stringResource(R.string.recovery_err_dup_answers)
        RecoveryPolicy.Problem.WRONG_COUNT, null -> null
    }
    val labels = listOf(
        R.string.recovery_question_1, R.string.recovery_answer_1,
        R.string.recovery_question_2, R.string.recovery_answer_2,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recovery_setup_title)) },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.recovery_setup_hint), style = MaterialTheme.typography.bodySmall)
                labels.forEachIndexed { i, label ->
                    OutlinedTextField(
                        value = fields[i],
                        onValueChange = { fields[i] = it; problem = null },
                        label = { Text(stringResource(label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    )
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && fields.all { it.isNotBlank() },
                onClick = {
                    scope.launch {
                        busy = true
                        val result = onSave(fields[0], fields[1], fields[2], fields[3])
                        if (result == null) onDismiss() else problem = result
                        busy = false
                    }
                },
            ) { Text(stringResource(R.string.settings_pin_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.recovery_skip)) } },
    )
}
